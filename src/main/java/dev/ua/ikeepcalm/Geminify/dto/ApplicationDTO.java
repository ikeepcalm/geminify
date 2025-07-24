package dev.ua.ikeepcalm.Geminify.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;

import java.time.LocalDateTime;
import java.util.List;

public class ApplicationDTO {
    @JsonProperty("user_id")
    private Long userId;
    
    private Long id;
    
    @JsonProperty("birth_date")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime birthDate;
    
    private String launcher;
    
    @JsonProperty("community_projects_readiness")
    private String communityProjectsReadiness;
    
    @JsonProperty("quiz_answer")
    private String quizAnswer;
    
    @JsonProperty("server_source")
    private String serverSource;
    
    @JsonProperty("conflict_reaction")
    private String conflictReaction;
    
    @JsonProperty("private_server_experience")
    private String privateServerExperience;
    
    @JsonProperty("healthy_community_definition")
    private String healthyCommunityDefinition;
    
    @JsonProperty("ideal_server_description")
    private String idealServerDescription;
    
    @JsonProperty("long_project_experience")
    private String longProjectExperience;
    
    @JsonProperty("useful_skills")
    private String usefulSkills;
    
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

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public LocalDateTime getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDateTime birthDate) { this.birthDate = birthDate; }
    
    public String getLauncher() { return launcher; }
    public void setLauncher(String launcher) { this.launcher = launcher; }
    
    public String getCommunityProjectsReadiness() { return communityProjectsReadiness; }
    public void setCommunityProjectsReadiness(String communityProjectsReadiness) { this.communityProjectsReadiness = communityProjectsReadiness; }
    
    public String getQuizAnswer() { return quizAnswer; }
    public void setQuizAnswer(String quizAnswer) { this.quizAnswer = quizAnswer; }
    
    public String getServerSource() { return serverSource; }
    public void setServerSource(String serverSource) { this.serverSource = serverSource; }
    
    public String getConflictReaction() { return conflictReaction; }
    public void setConflictReaction(String conflictReaction) { this.conflictReaction = conflictReaction; }
    
    public String getPrivateServerExperience() { return privateServerExperience; }
    public void setPrivateServerExperience(String privateServerExperience) { this.privateServerExperience = privateServerExperience; }
    
    public String getHealthyCommunityDefinition() { return healthyCommunityDefinition; }
    public void setHealthyCommunityDefinition(String healthyCommunityDefinition) { this.healthyCommunityDefinition = healthyCommunityDefinition; }
    
    public String getIdealServerDescription() { return idealServerDescription; }
    public void setIdealServerDescription(String idealServerDescription) { this.idealServerDescription = idealServerDescription; }
    
    public String getLongProjectExperience() { return longProjectExperience; }
    public void setLongProjectExperience(String longProjectExperience) { this.longProjectExperience = longProjectExperience; }
    
    public String getUsefulSkills() { return usefulSkills; }
    public void setUsefulSkills(String usefulSkills) { this.usefulSkills = usefulSkills; }
    
    public String getUsefulSkillsDetailed() { return usefulSkillsDetailed; }
    public void setUsefulSkillsDetailed(String usefulSkillsDetailed) { this.usefulSkillsDetailed = usefulSkillsDetailed; }
    
    public String getNewRuleReaction() { return newRuleReaction; }
    public void setNewRuleReaction(String newRuleReaction) { this.newRuleReaction = newRuleReaction; }
    
    public String getAdminDecisionAttitude() { return adminDecisionAttitude; }
    public void setAdminDecisionAttitude(String adminDecisionAttitude) { this.adminDecisionAttitude = adminDecisionAttitude; }
    
    public String getServerExperienceNegative() { return serverExperienceNegative; }
    public void setServerExperienceNegative(String serverExperienceNegative) { this.serverExperienceNegative = serverExperienceNegative; }
    
    public String getRussianWordReaction() { return russianWordReaction; }
    public void setRussianWordReaction(String russianWordReaction) { this.russianWordReaction = russianWordReaction; }
    
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    
    public List<String> getEditableFields() { return editableFields; }
    public void setEditableFields(List<String> editableFields) { this.editableFields = editableFields; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}