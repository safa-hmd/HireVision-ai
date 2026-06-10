package com.projet.hirevisionai.Dto;


import com.projet.hirevisionai.Entity.Role;

public record CompleteGoogleRegisterRequest(
        String email,
        String fullName,
        Role role
) {}