package com.ademarli.municipality_service.model.dto.auth;



public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {}


