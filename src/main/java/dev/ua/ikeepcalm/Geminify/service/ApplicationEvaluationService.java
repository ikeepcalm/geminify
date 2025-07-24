package dev.ua.ikeepcalm.Geminify.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ua.ikeepcalm.Geminify.dto.ApplicationDTO;
import dev.ua.ikeepcalm.Geminify.dto.EvaluationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ApplicationEvaluationService.class);

    private final WebClient webClient;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String geminiApiUrl;

    private static final List<String> BANNED_LAUNCHERS = Arrays.asList("tlauncher", "klauncher", "tlegacy");
    private static final int MIN_AGE = 14;
    private static final String CACHE_PREFIX = "eval:";

    public ApplicationEvaluationService(WebClient webClient, RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Mono<EvaluationResponse> evaluateApplication(ApplicationDTO application, boolean forceRefresh) {
        log.info("Starting evaluation for application ID: {}, forceRefresh: {}", application.getId(), forceRefresh);
        String cacheKey = CACHE_PREFIX + application.getId();

        if (!forceRefresh) {
            EvaluationResponse cached = (EvaluationResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.info("Found cached result for application: {}", application.getId());
                cached.setCached(true);
                return Mono.just(cached);
            }
        }

        EvaluationResponse quickReject = performQuickValidation(application);
        if (quickReject != null) {
            log.info("Quick validation rejected application: {}", quickReject.getReasoning());
            cacheResult(cacheKey, quickReject);
            return Mono.just(quickReject);
        }

        log.info("Proceeding with AI evaluation for application: {}", application.getId());
        return evaluateWithAI(application).map(response -> {
            log.info("Response from LLM: {}", response);
            cacheResult(cacheKey, response);
            return response;
        }).onErrorReturn(new EvaluationResponse("DECLINE", "AI evaluation failed", 0.5, false));
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
            if (BANNED_LAUNCHERS.stream().anyMatch(launcher::equalsIgnoreCase)) {
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

        return webClient.post().uri(geminiApiUrl + "?key=" + geminiApiKey).header("Content-Type", "application/json").bodyValue(requestBody).retrieve().bodyToMono(String.class).map(this::parseGeminiResponse);
    }

    private String buildEvaluationPrompt(ApplicationDTO app) {
        int age = app.getBirthDate() != null ? Period.between(app.getBirthDate().toLocalDate(), LocalDateTime.now().toLocalDate()).getYears() : -1;

        return String.format("""
                Evaluate this Minecraft server application. Respond ONLY with JSON format: {"recommendation": "ACCEPT|DECLINE", "reasoning": "brief explanation", "confidence": 0.0-1.0}
                
                CRITERIA:
                - Age %d (14+ required, older preferred for adult community)
                - Launcher: %s (Russian launchers auto-decline)
                - Version: %s
                - Answers must be detailed, well-punctuated, genuine, show interest
                - Poor punctuation = most probably auto-decline (no capitals, commas, periods)
                - Age should correlate with answer maturity
                
                APPLICATION ANSWERS:
                Server Source: "%s"
                Quiz Answer: "%s"
                
                SURVIVAL SECTION (Wiped server for casual play):
                Russian Word Reaction: "%s"
                Admin Decision Attitude: "%s"
                Conflict Reaction: "%s"
                New Rule Reaction: "%s"
                Negative Server Experience: "%s"
                Useful Skills: "%s"
                Useful Skills Detailed: "%s"
                
                EVERVAULT SECTION (Permanent server for long-term projects):
                Community Projects Readiness: "%s"
                Healthy Community Definition: "%s"
                Ideal Server Description: "%s"
                Long Project Experience: "%s"
                Private Server Experience: "%s"
                
                Application has two sections: Survival and Evervault. Each section has its own questions, so when evaluating, consider that if some answers are not provided, it may be due to the applicant not filling out that section. This is normal and should not be considered a negative factor.
                Main section applicable to both: age, launcher, server source, version.
                Survival section: russian word reaction, admin decision attitude, conflict reaction, new rule attitude, negative server experience, useful skills.
                Evervault section: community projects readiness, healthy community definition, ideal server description, long project experience, private server experience.
                
                Evervault is the server without wipes, where players can build and create long-term projects. Survival is the server with wipes, where players can play in a more casual way. Some answers may signal that the applicant is more suitable for one server type than the other, but this is not a strict requirement. You can recommend them for both servers if they meet the criteria, or for one if they are more suitable for it.
                
                Focus on answer quality, punctuation, maturity level matching stated age, and genuine interest. Make sure the answers are made by human, not generated via LLM. The reasoning in the response must be in Ukrainian language.
                """, age, 
                app.getLauncher(), 
                truncate(app.getVersion()),
                truncate(app.getServerSource()), 
                truncate(app.getQuizAnswer()),
                truncate(app.getRussianWordReaction()),
                truncate(app.getAdminDecisionAttitude()),
                truncate(app.getConflictReaction()),
                truncate(app.getNewRuleReaction()),
                truncate(app.getServerExperienceNegative()),
                truncate(app.getUsefulSkills()),
                truncate(app.getUsefulSkillsDetailed()),
                truncate(app.getCommunityProjectsReadiness()),
                truncate(app.getHealthyCommunityDefinition()),
                truncate(app.getIdealServerDescription()),
                truncate(app.getLongProjectExperience()),
                truncate(app.getPrivateServerExperience()));
    }

    private String truncate(String text) {
        if (text == null) return "Not provided";
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private String cleanMarkdownJson(String response) {
        if (response == null || response.trim().isEmpty()) {
            return response;
        }
        
        String cleaned = response.trim();
        
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        
        return cleaned.trim();
    }

    private EvaluationResponse parseGeminiResponse(String response) {
        try {
            log.info("Raw Gemini API Response: {}", response);
            JsonNode root = objectMapper.readTree(response);

            JsonNode content = root.path("candidates").get(0).path("content").path("parts").get(0).path("text");
            String aiResponse = content.asText();
            log.info("Extracted AI Response: {}", aiResponse);

            if (aiResponse.isEmpty()) {
                log.warn("Empty AI response from Gemini API");
                return new EvaluationResponse("DECLINE", "Empty AI response", 0.5, false);
            }

            String cleanedResponse = cleanMarkdownJson(aiResponse);
            log.info("Cleaned AI Response: {}", cleanedResponse);

            JsonNode parsed = objectMapper.readTree(cleanedResponse);
            EvaluationResponse result = new EvaluationResponse(parsed.path("recommendation").asText("DECLINE"), parsed.path("reasoning").asText("Failed to parse AI response"), parsed.path("confidence").asDouble(0.5), false);
            log.info("Parsed evaluation result: {}", result);
            return result;
        } catch (JsonProcessingException e) {
            log.error("JSON parsing error: {}", e.getMessage());
            return new EvaluationResponse("DECLINE", "AI response parsing failed - [" + e.getMessage() + "]", 0.5, false);
        } catch (Exception e) {
            log.error("General parsing error: {}", e.getMessage(), e);
            return new EvaluationResponse("DECLINE", "AI response parsing failed - [" + e.getMessage() + "]", 0.5, false);
        }
    }

    private void cacheResult(String cacheKey, EvaluationResponse response) {
        redisTemplate.opsForValue().set(cacheKey, response, 24, TimeUnit.HOURS);
    }
}