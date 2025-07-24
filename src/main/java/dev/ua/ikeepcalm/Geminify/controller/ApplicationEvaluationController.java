package dev.ua.ikeepcalm.Geminify.controller;

import dev.ua.ikeepcalm.Geminify.dto.ApplicationDTO;
import dev.ua.ikeepcalm.Geminify.dto.EvaluationResponse;
import dev.ua.ikeepcalm.Geminify.service.ApplicationEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@Tag(name = "Application Evaluation", description = "AI-powered Minecraft server application evaluation")
public class ApplicationEvaluationController {

    private static final Logger log = LoggerFactory.getLogger(ApplicationEvaluationController.class);
    private final ApplicationEvaluationService evaluationService;

    public ApplicationEvaluationController(ApplicationEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping("/evaluate")
    @Operation(summary = "Evaluate player application",
            description = "Analyzes application using AI to recommend ACCEPT or DECLINE")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evaluation completed successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ResponseEntity<EvaluationResponse>> evaluateApplication(
            @RequestBody ApplicationDTO application,
            @Parameter(description = "Force refresh cached result")
            @RequestParam(defaultValue = "false") boolean refresh) {
        log.info("Starting evaluation for application ID: {}, forceRefresh: {}", application.getId(), refresh);
        return evaluationService.evaluateApplication(application, refresh)
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build());
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Service health status")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Service is running");
    }
}