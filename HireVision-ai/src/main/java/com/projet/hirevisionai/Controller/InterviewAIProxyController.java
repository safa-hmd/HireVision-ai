package com.projet.hirevisionai.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Proxy controller — routes ALL Angular ↔ Python AI calls through Spring Boot.
 * Angular never calls Python directly; it always goes: Angular → Spring :8086 → Python :8000
 */
@RestController
@RequestMapping("/interview")
@RequiredArgsConstructor
public class InterviewAIProxyController {

    private final RestTemplate restTemplate;
    private static final String PYTHON_BASE = "http://localhost:8000";

    // ── 1. GET interview questions by specialty (AI-generated) ──────────────
    @GetMapping("/questions/{specialtyId}")
    public ResponseEntity<Map> getQuestions(@PathVariable String specialtyId) {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    PYTHON_BASE + "/interview/questions/" + specialtyId, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Python AI service unavailable: " + e.getMessage()));
        }
    }

    // ── 2. POST analyze voice response (NLP scoring via Gemini) ─────────────
    @PostMapping("/analyze-voice")
    public ResponseEntity<Map> analyzeVoice(@RequestBody Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_BASE + "/interview/analyze-voice",
                    HttpMethod.POST, entity, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            // Graceful fallback scores so Angular doesn't crash
            return ResponseEntity.ok(Map.of(
                    "score_technique",     70,
                    "score_communication", 70,
                    "score_confiance",     70,
                    "score_global",        70,
                    "niveau",              "Bien",
                    "points_forts",        "Réponse enregistrée",
                    "points_ameliorer",    "Service IA temporairement indisponible",
                    "reponse_ideale",      ""
            ));
        }
    }

    // ── 3. POST analyze webcam frame (computer vision) ───────────────────────
    @PostMapping(value = "/analyze-frame", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map> analyzeFrame(@RequestParam("frame") MultipartFile frame) {
        try {
            // Forward multipart to Python
            org.springframework.core.io.ByteArrayResource resource =
                    new org.springframework.core.io.ByteArrayResource(frame.getBytes()) {
                        @Override public String getFilename() { return "frame.jpg"; }
                    };

            org.springframework.util.MultiValueMap<String, Object> bodyMap =
                    new org.springframework.util.LinkedMultiValueMap<>();
            bodyMap.add("frame", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<org.springframework.util.MultiValueMap<String, Object>> entity =
                    new HttpEntity<>(bodyMap, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_BASE + "/interview/analyze-frame",
                    HttpMethod.POST, entity, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            // Fallback: return neutral scores (Angular will use simulation)
            return ResponseEntity.ok(Map.of(
                    "eye_contact", 80,
                    "posture",     75,
                    "engagement",  85,
                    "tips",        java.util.List.of(
                            "Maintenez le contact visuel avec la caméra",
                            "Parlez clairement et avec confiance"
                    )
            ));
        }
    }

    // ── 4. POST generate final feedback report ───────────────────────────────
    @PostMapping("/feedback")
    public ResponseEntity<Map> generateFeedback(@RequestBody Map<String, Object> body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_BASE + "/interview/feedback",
                    HttpMethod.POST, entity, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "titre",            "Rapport généré",
                    "message_global",   "Votre entretien a été analysé.",
                    "points_forts",     java.util.List.of("Bonne participation", "Communication claire"),
                    "axes_amelioration", java.util.List.of("Approfondissez les concepts techniques"),
                    "conseil_final",    "Continuez à pratiquer régulièrement !",
                    "recommandation",   "En bonne voie"
            ));
        }
    }

    // ── 5. GET specialties list ──────────────────────────────────────────────
    @GetMapping("/specialties")
    public ResponseEntity<Object> getSpecialties() {
        try {
            ResponseEntity<Object> response = restTemplate.getForEntity(
                    PYTHON_BASE + "/interview/specialties", Object.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}