package com.moon.rabbit.domain.auth.dto;


public record GoogleUserInfo(
        String id,
        String email,
        String name,
        String picture
) {}