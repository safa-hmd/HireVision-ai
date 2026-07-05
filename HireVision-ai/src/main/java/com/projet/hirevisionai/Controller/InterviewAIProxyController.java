package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Repository.InterviewRepository;
import com.projet.hirevisionai.Repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Proxy controller — routes ALL Angular AI calls through Spring Boot.
 * Angular → Spring :8086/HireVision/interview/* → Python :8000/interview/*
 *
 * Améliorations v2 :
 *  - Passe userId pour exclure les questions déjà posées (anti-répétition)
 *  - Route /next-question pour difficulté progressive
 *  - Fallback systématique si Python indisponible
 */
@RestController
@RequestMapping("/interview")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class InterviewAIProxyController {

    private final RestTemplate        restTemplate;
    private final InterviewRepository interviewRepository;
    private final QuestionRepository  questionRepository;

    private static final String PYTHON_BASE = "http://localhost:8000";

    // ── 1. GET questions par spécialité (anti-répétition) ───────────────────
    @GetMapping("/questions/{specialtyId}")
    public ResponseEntity<Map> getQuestions(
            @PathVariable String specialtyId,
            @RequestParam(required = false) Long userId) {
        try {
            // Construire la liste des questions déjà posées à cet utilisateur
            String excludedParam = "";
            if (userId != null) {
                List<String> alreadyAsked = getAlreadyAskedQuestions(userId);
                if (!alreadyAsked.isEmpty()) {
                    // Encoder en JSON pour le query param
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < alreadyAsked.size(); i++) {
                        sb.append("\"")
                                .append(alreadyAsked.get(i).replace("\"", "\\\""))
                                .append("\"");
                        if (i < alreadyAsked.size() - 1) sb.append(",");
                    }
                    sb.append("]");
                    try {
                        excludedParam = "&excluded_questions="
                                + java.net.URLEncoder.encode(sb.toString(), "UTF-8");
                    } catch (Exception e) {
                        excludedParam = "";
                    }
                }
            }

            String url = PYTHON_BASE + "/interview/questions/" + specialtyId
                    + (userId != null ? "?user_id=" + userId : "?")
                    + excludedParam;

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Python AI service unavailable: " + e.getMessage(),
                            "questions", List.of()));
        }
    }

    // ── 2. POST analyse vocale ────────────────────────────────────────────────
    @PostMapping("/analyze-voice")
    public ResponseEntity<Map> analyzeVoice(@RequestBody Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> entity = jsonEntity(body);
            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_BASE + "/interview/analyze-voice",
                    HttpMethod.POST, entity, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            // Fallback scores pour ne pas bloquer Angular
            return ResponseEntity.ok(Map.of(
                    "score_technique",     70,
                    "score_communication", 70,
                    "score_confiance",     70,
                    "score_global",        70,
                    "niveau",              "Bien",
                    "points_forts",        "Réponse enregistrée",
                    "points_ameliorer",    "Service IA temporairement indisponible",
                    "reponse_ideale",      "",
                    "analyse_vocale",      Map.of(
                            "nb_mots", 0,
                            "hesitations", 0,
                            "conseil_immediat", "Parlez clairement et avec confiance"
                    )
            ));
        }
    }

    // ── 3. POST analyse frame webcam (computer vision) ───────────────────────
    @PostMapping(value = "/analyze-frame", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map> analyzeFrame(@RequestParam("frame") MultipartFile frame) {
        try {
            ByteArrayResource resource = new ByteArrayResource(frame.getBytes()) {
                @Override public String getFilename() { return "frame.jpg"; }
            };
            MultiValueMap<String, Object> bodyMap = new LinkedMultiValueMap<>();
            bodyMap.add("frame", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(bodyMap, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_BASE + "/interview/analyze-frame",
                    HttpMethod.POST, entity, Map.class
            );
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            // Fallback métriques neutres
            return ResponseEntity.ok(Map.of(
                    "eye_contact",    80,
                    "posture",        75,
                    "engagement",     85,
                    "face_detected",  false,
                    "tips",           List.of(
                            "Maintenez le contact visuel avec la caméra",
                            "Parlez clairement et avec confiance"
                    )
            ));
        }
    }

    // ── 4. POST feedback final + plan d'apprentissage ────────────────────────
    @PostMapping("/feedback")
    public ResponseEntity<Map> generateFeedback(@RequestBody Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> entity = jsonEntity(body);
            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_BASE + "/interview/feedback",
                    HttpMethod.POST, entity, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "titre",             "Rapport généré",
                    "message_global",    "Votre entretien a été analysé.",
                    "points_forts",      List.of("Bonne participation", "Communication claire"),
                    "axes_amelioration", List.of("Approfondissez les concepts techniques"),
                    "conseil_final",     "Continuez à pratiquer régulièrement !",
                    "recommandation",    "En bonne voie",
                    "plan_apprentissage", List.of(
                            Map.of("semaine", 1, "theme", "Révision des bases",
                                    "ressource", "Documentation officielle",
                                    "action",    "Lire et résumer les concepts clés"),
                            Map.of("semaine", 2, "theme", "Projets pratiques",
                                    "ressource", "GitHub / Exercices",
                                    "action",    "Coder un mini-projet en 48h"),
                            Map.of("semaine", 3, "theme", "Entretiens simulés",
                                    "ressource", "HireVision AI",
                                    "action",    "Refaire 3 simulations d'entretien")
                    )
            ));
        }
    }

    // ── 5. POST prochaine question adaptative (difficulté progressive) ────────
    @PostMapping("/next-question")
    public ResponseEntity<Map> getNextQuestion(@RequestBody Map<String, Object> body) {
        try {
            HttpEntity<Map<String, Object>> entity = jsonEntity(body);
            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_BASE + "/interview/next-question",
                    HttpMethod.POST, entity, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Service unavailable", "finished", false));
        }
    }

    // ── 6. GET liste des spécialités ─────────────────────────────────────────
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

    // ── 7. DELETE cache Python (admin/dev) ───────────────────────────────────
    @DeleteMapping("/cache/{specialtyId}")
    public ResponseEntity<Map> clearCache(@PathVariable String specialtyId) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_BASE + "/interview/cache/" + specialtyId,
                    HttpMethod.DELETE, null, Map.class
            );
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Cache endpoint non disponible"));
        }
    }

    // ── Helper : récupérer questions déjà posées à userId ────────────────────
    private List<String> getAlreadyAskedQuestions(Long userId) {
        try {
            // Récupère tous les entretiens de l'utilisateur
            List<com.projet.hirevisionai.Entity.Interview> interviews =
                    interviewRepository.findByUserIdUserOrderByStartDateDesc(userId);

            List<String> questions = new ArrayList<>();
            for (com.projet.hirevisionai.Entity.Interview interview : interviews) {
                questionRepository.findByInterviewId(interview.getId())
                        .forEach(q -> {
                            if (q.getContent() != null && !q.getContent().isBlank()) {
                                questions.add(q.getContent());
                            }
                        });
            }
            return questions;
        } catch (Exception e) {
            return List.of(); // fallback silencieux
        }
    }

    // ── Helper : créer une HttpEntity JSON ───────────────────────────────────
    private HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}