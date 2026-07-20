package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.JobMatchRequestDTO;
import com.projet.hirevisionai.Dto.MatchingResultDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.JobOffer;
import com.projet.hirevisionai.Entity.MatchingResult;
import com.projet.hirevisionai.Entity.MissedSkill;
import com.projet.hirevisionai.Entity.Skill;
import com.projet.hirevisionai.Entity.SkillPriority;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.JobOfferRepository;
import com.projet.hirevisionai.Repository.MatchingResultRepository;
import com.projet.hirevisionai.Repository.MissedSkillRepository;
import com.projet.hirevisionai.ServiceInterface.IMatchingResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingResultServiceImpl implements IMatchingResultService {

    private final MatchingResultRepository matchingResultRepository;
    private final CvRepository             cvRepository;
    private final MissedSkillRepository missedSkillRepository;
    private final JobOfferRepository jobOfferRepository;
    private final RestTemplate restTemplate;

    @Override
    public List<MatchingResultDTO> getByCvId(Long cvId) {
        return matchingResultRepository.findByCvId(cvId)
                .stream().map(MatchingResultDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<MatchingResultDTO> getByUserId(Long userId) {
        return matchingResultRepository.findByCvUserIdUser(userId)
                .stream().map(MatchingResultDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public MatchingResultDTO getBestByCvId(Long cvId) {
        return MatchingResultDTO.fromEntity(
                matchingResultRepository.findTopByCvIdOrderByScoreDesc(cvId)
                        .orElseThrow(() -> new RuntimeException("Aucun résultat pour le CV : " + cvId)));
    }



    @Override
    public MatchingResultDTO matchAndSave(JobMatchRequestDTO request) {

        List<String> cvSkills = request.getCvSkills();
        if (cvSkills == null) cvSkills = List.of();

        // ── Si une offre d'emploi est référencée, on utilise ses compétences requises ──
        JobOffer jobOffer = null;
        List<String> jobSkills;
        if (request.getJobOfferId() != null) {
            jobOffer = jobOfferRepository.findById(request.getJobOfferId())
                    .orElseThrow(() -> new RuntimeException("Offre d'emploi introuvable : " + request.getJobOfferId()));
            jobSkills = jobOffer.getRequiredSkills().stream()
                    .map(Skill::getName)
                    .collect(Collectors.toList());
        } else {
            jobSkills = request.getJobSkills();
            if (jobSkills == null) jobSkills = List.of();
        }

        // ── Appeler Python en JSON (pas multipart) ──────────────────
        Map<String, Object> body = new HashMap<>();
        body.put("cv_skills",  cvSkills);
        body.put("job_skills", jobSkills);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);  // ← JSON pas multipart

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        Map pythonResult;
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    "http://localhost:8000/match-job",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            pythonResult = response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erreur microservice Python: " + e.getMessage());
        }

        // ── Extraire les résultats ──────────────────────────────────
        float score          = ((Number) pythonResult.get("score")).floatValue();
        List<String> missing = (List<String>) pythonResult.getOrDefault("missing", List.of());
        List<String> matched = (List<String>) pythonResult.getOrDefault("matched", List.of());

        // ── Sauvegarder en DB ───────────────────────────────────────
        CV cv = cvRepository.findById(request.getCvId())
                .orElseThrow(() -> new RuntimeException("CV introuvable: " + request.getCvId()));

        MatchingResult saved = matchingResultRepository.save(
                MatchingResult.builder()
                        .score(score)
                        .cv(cv)
                        .jobOffer(jobOffer)
                        .build()
        );

        // ── Persister les compétences manquantes (pour "Plan de Carrière") ──
        // Heuristique simple : les 1ers tiers de la liste = priorité haute
        // (généralement les compétences les plus citées dans l'offre), etc.
        int total = missing.size();
        for (int i = 0; i < total; i++) {
            SkillPriority priority;
            int estimatedWeeks;
            double ratio = total > 0 ? (double) i / total : 0;
            if (ratio < 0.34) {
                priority = SkillPriority.HAUTE;
                estimatedWeeks = 1;
            } else if (ratio < 0.67) {
                priority = SkillPriority.MOYENNE;
                estimatedWeeks = 2;
            } else {
                priority = SkillPriority.BASSE;
                estimatedWeeks = 3;
            }

            missedSkillRepository.save(
                    MissedSkill.builder()
                            .skillName(missing.get(i))
                            .priority(priority)
                            .estimatedWeeks(estimatedWeeks)
                            .matchingResult(saved)
                            .build()
            );
        }

        // ── Retourner DTO enrichi ───────────────────────────────────
        MatchingResultDTO dto = MatchingResultDTO.fromEntity(saved);
        dto.setLabel((String)   pythonResult.getOrDefault("label",     ""));
        dto.setMessage((String) pythonResult.getOrDefault("message",   ""));
        dto.setCompatible((Boolean) pythonResult.getOrDefault("compatible", false));
        dto.setMatched(matched);
        dto.setMissingSkills(missing);
        return dto;
    }
}