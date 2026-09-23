package com.hyeja.domain.cardnews.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder 
@Getter 
@ToString 
@NoArgsConstructor 
@AllArgsConstructor 
public class CardNewsRequestDTO {
    private int limit = 4;
    private String sort = "deadline";
}