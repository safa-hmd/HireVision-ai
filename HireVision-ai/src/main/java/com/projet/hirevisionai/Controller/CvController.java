package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.CvDTO;
import com.projet.hirevisionai.Dto.CvUploadResponseDTO;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.ICvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("cvs")
@RequiredArgsConstructor
public class CvController {

    private final ICvService cvService;
    private final UserRepository userRepository;

    // ── Récupère l'utilisateur actuellement connecté (via le token JWT) ──
    private User getConnectedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur introuvable"));
    }

    // ── Vérifie que l'utilisateur connecté a le droit d'accéder aux données de "targetUserId" ──
    private void checkOwnership(Long targetUserId) {
        User connected = getConnectedUser();
        boolean isOwner = connected.getIdUser().equals(targetUserId);
        boolean isAdmin = connected.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à ce CV");
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<CvDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        checkOwnership(userId);
        return ResponseEntity.ok(cvService.upload(file, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CvDTO> getById(@PathVariable Long id) {
        CvDTO cv = cvService.getById(id);
        checkOwnership(cv.getUserId());
        return ResponseEntity.ok(cv);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CvDTO>> getByUserId(@PathVariable Long userId) {
        checkOwnership(userId);
        return ResponseEntity.ok(cvService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<CvDTO> getLatest(@PathVariable Long userId) {
        checkOwnership(userId);
        return ResponseEntity.ok(cvService.getLatestByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        CvDTO cv = cvService.getById(id);
        checkOwnership(cv.getUserId());
        cvService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload-and-analyze")
    public ResponseEntity<CvUploadResponseDTO> uploadAndAnalyze(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        checkOwnership(userId);
        return ResponseEntity.ok(cvService.uploadAndAnalyze(file, userId));
    }

    @GetMapping("/user/{userId}/latest-analysis")
    public ResponseEntity<CvUploadResponseDTO> getLatestAnalysis(@PathVariable Long userId) {
        checkOwnership(userId);
        return ResponseEntity.ok(cvService.getLatestAnalysis(userId));
    }
}