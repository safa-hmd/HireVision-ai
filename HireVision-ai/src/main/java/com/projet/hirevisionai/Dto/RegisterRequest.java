package com.projet.hirevisionai.Dto;


import com.projet.hirevisionai.Entity.Role;

public record RegisterRequest(
        String fullName,
        String email,
        String password,
        Integer age,
        Role role
) {}
