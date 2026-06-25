package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.CvDTO;
import com.projet.hirevisionai.Entity.CV;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.CvRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.ICvService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CvServiceImpl implements ICvService {

    private final CvRepository   cvRepository;
    private final UserRepository userRepository;

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
}