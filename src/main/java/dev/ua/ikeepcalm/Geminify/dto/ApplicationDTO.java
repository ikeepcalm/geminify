package dev.ua.ikeepcalm.Geminify.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Setter
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationDTO {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("birth_date")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime birthDate;

    @JsonProperty("launcher")
    private String launcher;

    @JsonProperty("community_projects_readiness")
    private String communityProjectsReadiness;

    @JsonProperty("server_source")
    private String serverSource;

    @JsonProperty("healthy_community_definition")
    private String healthyCommunityDefinition;

    @JsonProperty("ideal_server_description")
    private String idealServerDescription;

    @JsonProperty("long_project_experience")
    private String longProjectExperience;

    @JsonProperty("useful_skills_detailed")
    private String usefulSkillsDetailed;

    @JsonProperty("new_rule_reaction")
    private String newRuleReaction;

    @JsonProperty("admin_decision_attitude")
    private String adminDecisionAttitude;

    @JsonProperty("server_experience_negative")
    private String serverExperienceNegative;

    @JsonProperty("russian_word_reaction")
    private String russianWordReaction;

    private String version;

    @JsonProperty("editable_fields")
    private List<String> editableFields;

    @JsonProperty("updated_at")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

}