package dev.ua.ikeepcalm.Geminify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EvaluationResponse {
    private String recommendation;
    private String reasoning;
    private double confidence;

    @JsonProperty("is_cached")
    private boolean isCached;

    public EvaluationResponse() {}

    public EvaluationResponse(String recommendation, String reasoning, double confidence, boolean isCached) {
        this.recommendation = recommendation;
        this.reasoning = reasoning;
        this.confidence = confidence;
        this.isCached = isCached;
    }

    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String recommendation) { this.recommendation = recommendation; }

    public String getReasoning() { return reasoning; }
    public void setReasoning(String reasoning) { this.reasoning = reasoning; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isCached() { return isCached; }
    public void setCached(boolean cached) { isCached = cached; }
}