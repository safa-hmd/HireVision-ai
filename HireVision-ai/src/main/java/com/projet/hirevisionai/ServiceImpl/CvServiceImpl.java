package com.projet.hirevisionai.ServiceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projet.hirevisionai.Dto.CvAnalysisDTO;
import com.projet.hirevisionai.Dto.CvDTO;
import com.projet.hirevisionai.Dto.CvUploadResponseDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.Skill;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.SkillRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.ICvService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ByteArrayResource;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CvServiceImpl implements ICvService {

    private final RestTemplate restTemplate;
    private final CvRepository   cvRepository;
    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ObjectMapper objectMapper;

    private static final String UPLOAD_DIR = "uploads/cvs/";

    @Override
    public CvDTO upload(MultipartFile file, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path   path     = Paths.get(UPLOAD_DIR + fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            CV cv = CV.builder()
                    .filePath(path.toString())
                    .uploadDate(LocalDate.now())
                    .user(user)
                    .build();

            return CvDTO.fromEntity(cvRepository.save(cv));

        } catch (IOException e) {
            throw new RuntimeException("Erreur upload fichier: " + e.getMessage());
        }
    }

    @Override
    public CvDTO getById(Long id) {
        return CvDTO.fromEntity(
                cvRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("CV not found: " + id)));
    }

    @Override
    public List<CvDTO> getByUserId(Long userId) {
        return cvRepository.findByUserIdUser(userId)
                .stream()
                .map(CvDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public CvDTO getLatestByUserId(Long userId) {
        return CvDTO.fromEntity(
                cvRepository.findTopByUserIdUserOrderByUploadDateDesc(userId)
                        .orElseThrow(() -> new RuntimeException("No CV found for user: " + userId)));
    }

    @Override
    public void delete(Long id) {
        if (!cvRepository.existsById(id))
            throw new RuntimeException("CV not found: " + id);
        cvRepository.deleteById(id);
    }

    @Override
    public CvUploadResponseDTO uploadAndAnalyze(MultipartFile file, Long userId) {

        // 1. Sauvegarder le fichier
        CvDTO savedCv = upload(file, userId);
        CV cv = cvRepository.findById(savedCv.getId())
                .orElseThrow(() -> new RuntimeException("CV introuvable"));

        // 2. Appeler Python
        byte[] fileBytes;
        try { fileBytes = file.getBytes(); }
        catch (IOException e) { throw new RuntimeException(e); }

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(fileBytes) {
            @Override public String getFilename() { return file.getOriginalFilename(); }
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        ResponseEntity<CvAnalysisDTO> response = restTemplate.exchange(
                "http://localhost:8000/analyze",
                HttpMethod.POST,
                requestEntity,
                CvAnalysisDTO.class
        );

        CvAnalysisDTO analysis = response.getBody();

        // 3. Sauvegarder les skills en DB
        if (analysis != null && analysis.getSkills() != null) {
            for (String skillName : analysis.getSkills()) {
                Skill skill = skillRepository.findByName(skillName)
                        .orElseGet(() -> skillRepository.save(
                                Skill.builder()
                                        .name(skillName)
                                        .category("Technical")
                                        .build()
                        ));
                if (cv.getSkills() == null) {
                    cv.setSkills(new ArrayList<>());
                }
                if (!cv.getSkills().contains(skill)) {
                    cv.getSkills().add(skill);
                }
            }
        }

        // 4. Sauvegarder l'analyse complète en JSON pour la retrouver plus tard
        if (analysis != null) {
            try {
                cv.setAnalysisJson(objectMapper.writeValueAsString(analysis));
            } catch (Exception e) {
                // si la sérialisation échoue, on continue sans bloquer l'upload
            }
        }
        cvRepository.save(cv);

        return CvUploadResponseDTO.builder()
                .cv(CvDTO.fromEntity(cv))
                .analysis(analysis)
                .build();
    }

    @Override
    public CvUploadResponseDTO getLatestAnalysis(Long userId) {
        return cvRepository.findTopByUserIdUserOrderByUploadDateDesc(userId)
                .map(cv -> {
                    CvAnalysisDTO analysis = null;
                    if (cv.getAnalysisJson() != null) {
                        try {
                            analysis = objectMapper.readValue(cv.getAnalysisJson(), CvAnalysisDTO.class);
                        } catch (Exception e) {
                            analysis = null;
                        }
                    }
                    return CvUploadResponseDTO.builder()
                            .cv(CvDTO.fromEntity(cv))
                            .analysis(analysis)
                            .build();
                })
                .orElse(CvUploadResponseDTO.builder().cv(null).analysis(null).build());
    }
}