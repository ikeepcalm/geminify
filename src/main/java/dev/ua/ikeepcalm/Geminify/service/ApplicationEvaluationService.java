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
                return new EvaluationResponse("DECLINE", "Малий вік (має бути ПРИНАЙМНІ " + age + "+ років)", 1.0, false);
            }
        }

        if (application.getLauncher() != null) {
            String launcher = application.getLauncher().toLowerCase();
            if (BANNED_LAUNCHERS.stream().anyMatch(launcher::equalsIgnoreCase)) {
                return new EvaluationResponse("DECLINE", "Заборонений лаунчер, НЕ ЧИТАВ ПРАВИЛА: " + application.getLauncher(), 1.0, false);
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
                        "temperature": 0.3,
                        "maxOutputTokens": 1000
                    }
                }
                """, prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        return webClient.post().uri(geminiApiUrl + "?key=" + geminiApiKey).header("Content-Type", "application/json").bodyValue(requestBody).retrieve().bodyToMono(String.class).map(this::parseGeminiResponse);
    }

    private String buildEvaluationPrompt(ApplicationDTO app) {
        int age = app.getBirthDate() != null ? Period.between(app.getBirthDate().toLocalDate(), LocalDateTime.now().toLocalDate()).getYears() : -1;

        return String.format("""
                        Evaluate this Minecraft server application. Respond ONLY with a valid JSON object. No text or explanation outside of it.
                        
                        {"recommendation": "ACCEPT" | "DECLINE", "reasoning": "short explanation in Ukrainian", "confidence": 0.0-1.0}
                        
                        CRITERIA:
                        - Age: %d (Minimum 14; older age preferred for adult community. Maturity of answers must align with claimed age.)
                        - Launcher: %s (Russian-branded launchers or pirated versions lead to DECLINE.)
                        - Version: %s
                        - Answers must be human-written, detailed, well-punctuated, and show genuine interest.
                        - Poor punctuation (lack of capitalization, commas, periods) suggests lower effort or possible AI use.
                        - Do not penalize applicants for skipping entire sections (Survival or Evervault); they may be interested only in one server mode.
                        - Application may include typos or mixed Ukrainian/Russian words — do not auto-decline unless clear disrespect or consistent Russian usage is evident.
                        - Server is Ukrainian-only: applicants must support a Ukrainian-speaking, respectful, intelligent community. Occasional language slips are not enough for rejection.
                        
                        SECTIONS (Some may be empty — that’s acceptable):
                        
                        Server Source: "%s"
                        
                        SURVIVAL SECTION:
                        - Russian Word Reaction: "%s"
                        - Admin Decision Attitude: "%s"
                        - New Rule Reaction: "%s"
                        - Negative Server Experience: "%s"
                        - Useful Skills Detailed: "%s"
                        
                        EVERVAULT SECTION:
                        - Community Projects Readiness: "%s"
                        - Healthy Community Definition: "%s"
                        - Ideal Server Description: "%s"
                        - Long Project Experience: "%s"
                        
                        NOTES FOR EVALUATION:
                        - Survival is a casual, wipe-based experience.
                        - Evervault is a permanent world for long-term builds.
                        - Applicants may fit better in one mode than the other — recommend either or both.
                        - Focus on evaluating maturity, community fit, and language. Reject only if major red flags exist (e.g. Russian propaganda, disrespect, fake answers).
                        
                        The reasoning must be written in Ukrainian. Do not over-rely on the presence of Russian words unless clearly inappropriate or politically charged.
                        """, age,
                app.getLauncher(),
                truncate(app.getVersion()),
                truncate(app.getServerSource()),
                truncate(app.getRussianWordReaction()),
                truncate(app.getAdminDecisionAttitude()),
                truncate(app.getNewRuleReaction()),
                truncate(app.getServerExperienceNegative()),
                truncate(app.getUsefulSkillsDetailed()),
                truncate(app.getCommunityProjectsReadiness()),
                truncate(app.getHealthyCommunityDefinition()),
                truncate(app.getIdealServerDescription()),
                truncate(app.getLongProjectExperience()));
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