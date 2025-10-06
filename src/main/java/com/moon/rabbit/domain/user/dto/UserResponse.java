package com.moon.rabbit.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class UserResponse {
    private final String email;
    private final String score;
}
