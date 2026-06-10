package com.projet.hirevisionai.Dto;

public record ResetPasswordRequest(
        String token,
        String newPassword
) {}