package com.projet.hirevisionai.Exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Réponse structurée standard ───────────────────────────────────────────
    private Map<String, Object> buildError(int status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status",    status);
        body.put("message",   message);
        body.put("path",      path);
        return body;
    }

    // ── Validation @Valid / @Validated ────────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valeur invalide",
                        (a, b) -> a
                ));
        Map<String, Object> body = buildError(400, "Données de requête invalides", req.getRequestURI());
        body.put("errors", fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // ── Validation métier ─────────────────────────────────────────────────────
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<?> handleBusinessValidation(BusinessValidationException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(buildError(400, ex.getMessage(), req.getRequestURI()));
    }

    // ── IllegalArgument / IllegalState ───────────────────────────────────────
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<?> handleIllegalArgs(RuntimeException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest().body(buildError(400, ex.getMessage(), req.getRequestURI()));
    }

    // ── ResponseStatusException (ex: 404, 403 via @PathVariable) ─────────────
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> handleResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        int status = ex.getStatusCode().value();
        return ResponseEntity.status(status).body(buildError(status, ex.getReason(), req.getRequestURI()));
    }

    // ── Accès refusé (Spring Security) ───────────────────────────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(buildError(403, "Accès refusé : vous n'avez pas les droits nécessaires", req.getRequestURI()));
    }

    // ── Authentification (Spring Security) ───────────────────────────────────
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<?> handleAuthException(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(buildError(401, "Authentification requise : token manquant ou invalide", req.getRequestURI()));
    }

    // ── Fallback global ───────────────────────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception ex, HttpServletRequest req) {
        ex.printStackTrace();
        return ResponseEntity.internalServerError()
                .body(buildError(500, "Une erreur interne s'est produite. Veuillez réessayer.", req.getRequestURI()));
    }
}
