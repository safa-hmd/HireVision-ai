package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.User;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long   idUser;
    private String fullName;
    private String email;
    private int    age;
    private String role;

    // ── Mapping Entity → DTO ──────────────────────────────────────
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .idUser(user.getIdUser())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .age(user.getAge())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .build();
    }
}