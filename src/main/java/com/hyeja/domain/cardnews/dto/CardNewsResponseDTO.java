package com.hyeja.domain.cardnews.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder 
@Getter 
@AllArgsConstructor 
@NoArgsConstructor 
@ToString 
public class CardNewsResponseDTO {

    @JsonProperty("policy_id")
    private String policyId;

    @JsonProperty("policy_name")
    private String policyName;

    @JsonProperty("description") 
    private String description; 

    @JsonProperty("apply_end_date") 
    private String applyEndDate;
}