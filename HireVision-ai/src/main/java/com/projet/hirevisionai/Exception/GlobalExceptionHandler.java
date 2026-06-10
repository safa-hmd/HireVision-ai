package com.projet.hirevisionai.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<?> handleBusinessValidation(BusinessValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "timestamp", java.time.LocalDateTime.now().toString(),
                "status",    400,
                "error",     "Business Validation Error",
                "message",   ex.getMessage()
        ));
    }

    /**
     * Gère les IllegalArgumentException :
     * - Email requis / vide
     * - Email déjà utilisé
     * - Mot de passe trop court
     * - Rôle requis
     * - Token invalide / expiré / déjà utilisé
     * - Mot de passe actuel incorrect
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage()
        ));
    }

    /**
     * Gère les erreurs d'authentification Spring Security
     */
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<?> handleAuthException(Exception ex) {
        return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid credentials"
        ));
    }

    /**
     * Gère toutes les autres exceptions non prévues
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.internalServerError().body(Map.of(
                "error", "An unexpected error occurred: " + ex.getMessage()
        ));
    }
}
