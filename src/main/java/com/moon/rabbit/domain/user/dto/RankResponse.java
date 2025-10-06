package com.moon.rabbit.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankResponse {
    private int rank;
    private final String email;
    private final Integer score;
}