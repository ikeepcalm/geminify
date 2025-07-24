package dev.ua.ikeepcalm.Geminify.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ua.ikeepcalm.Geminify.dto.ApplicationDTO;
import dev.ua.ikeepcalm.Geminify.dto.EvaluationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ApplicationEvaluationService {

    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash-latest:generateContent}")
    private String geminiApiUrl;

    private static final List<String> BANNED_LAUNCHERS = Arrays.asList("tlauncher", "klauncher");
    private static final int MIN_AGE = 14;
    private static final String CACHE_PREFIX = "eval:";

    public ApplicationEvaluationService(WebClient webClient, RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<EvaluationResponse> evaluateApplication(ApplicationDTO application, boolean forceRefresh) {
        String cacheKey = CACHE_PREFIX + application.getId();
        
        if (!forceRefresh) {
            EvaluationResponse cached = (EvaluationResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                cached.setCached(true);
                return Mono.just(cached);
            }
        }

        EvaluationResponse quickReject = performQuickValidation(application);
        if (quickReject != null) {
            cacheResult(cacheKey, quickReject);
            return Mono.just(quickReject);
        }

        return evaluateWithAI(application)
                .map(response -> {
                    cacheResult(cacheKey, response);
                    return response;
                })
                .onErrorReturn(new EvaluationResponse("DECLINE", "AI evaluation failed", 0.5, false));
    }

    private EvaluationResponse performQuickValidation(ApplicationDTO application) {
        if (application.getBirthDate() != null) {
            int age = Period.between(application.getBirthDate().toLocalDate(), LocalDateTime.now().toLocalDate()).getYears();
            if (age < MIN_AGE) {
                return new EvaluationResponse("DECLINE", "Age below minimum requirement (" + age + " years)", 1.0, false);
            }
        }

        if (application.getLauncher() != null) {
            String launcher = application.getLauncher().toLowerCase();
            if (BANNED_LAUNCHERS.stream().anyMatch(launcher::contains)) {
                return new EvaluationResponse("DECLINE", "Using banned launcher: " + application.getLauncher(), 1.0, false);
            }
        }

        return null;
    }

    private Mono<EvaluationResponse> evaluateWithAI(ApplicationDTO application) {
        String prompt = buildEvaluationPrompt(application);
        
        String requestBody = String.format("""
            {
                "contents": [{
                    "parts": [{
                        "text": "%s"
                    }]
                }],
                "generationConfig": {
                    "temperature": 0.1,
                    "maxOutputTokens": 500
                }
            }
            """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        return webClient.post()
                .uri(geminiApiUrl + "?key=" + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseGeminiResponse);
    }

    private String buildEvaluationPrompt(ApplicationDTO app) {
        int age = app.getBirthDate() != null ? 
            Period.between(app.getBirthDate().toLocalDate(), LocalDateTime.now().toLocalDate()).getYears() : -1;

        return String.format("""
            Evaluate this Minecraft server application. Respond ONLY with JSON format: {"recommendation": "ACCEPT|DECLINE", "reasoning": "brief explanation", "confidence": 0.0-1.0}

            CRITERIA:
            - Age %d (14+ required, older preferred for adult community)
            - Launcher: %s (Russian launchers auto-decline)
            - Answers must be detailed, well-punctuated, genuine, show interest
            - Poor punctuation = auto-decline (no capitals, commas, periods)
            - Age should correlate with answer maturity

            APPLICATION ANSWERS:
            Community Projects: "%s"
            Quiz Answer: "%s"
            Server Source: "%s"
            Conflict Reaction: "%s"
            Server Experience: "%s"
            Community Definition: "%s"
            Ideal Server: "%s"
            Project Experience: "%s"
            Skills: "%s"

            Focus on answer quality, punctuation, maturity level matching stated age, and genuine interest.
            """, 
            age, 
            app.getLauncher(),
            truncate(app.getCommunityProjectsReadiness()),
            truncate(app.getQuizAnswer()),
            truncate(app.getServerSource()),
            truncate(app.getConflictReaction()),
            truncate(app.getPrivateServerExperience()),
            truncate(app.getHealthyCommunityDefinition()),
            truncate(app.getIdealServerDescription()),
            truncate(app.getLongProjectExperience()),
            truncate(app.getUsefulSkills())
        );
    }

    private String truncate(String text) {
        if (text == null) return "Not provided";
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private EvaluationResponse parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
            String aiResponse = content.asText();
            
            JsonNode parsed = objectMapper.readTree(aiResponse);
            return new EvaluationResponse(
                parsed.path("recommendation").asText("DECLINE"),
                parsed.path("reasoning").asText("Failed to parse AI response"),
                parsed.path("confidence").asDouble(0.5),
                false
            );
        } catch (JsonProcessingException e) {
            return new EvaluationResponse("DECLINE", "AI response parsing failed", 0.5, false);
        }
    }

    private void cacheResult(String cacheKey, EvaluationResponse response) {
        redisTemplate.opsForValue().set(cacheKey, response, 24, TimeUnit.HOURS);
    }
}