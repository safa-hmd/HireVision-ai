package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.CvDTO;
import com.projet.hirevisionai.Dto.JobMatchRequestDTO;
import com.projet.hirevisionai.Dto.MatchingResultDTO;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.ICvService;
import com.projet.hirevisionai.ServiceInterface.IMatchingResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/matching-results")
@RequiredArgsConstructor
public class MatchingResultController {

    private final IMatchingResultService matchingResultService;
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
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé à ce résultat de matching");
        }
    }

    // ── Vérifie que le CV donné appartient bien à l'utilisateur connecté (ou admin) ──
    private void checkOwnershipByCv(Long cvId) {
        CvDTO cv = cvService.getById(cvId);
        checkOwnership(cv.getUserId());
    }


    @GetMapping("/cv/{cvId}")
    public ResponseEntity<List<MatchingResultDTO>> getByCvId(@PathVariable Long cvId) {
        checkOwnershipByCv(cvId);
        return ResponseEntity.ok(matchingResultService.getByCvId(cvId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MatchingResultDTO>> getByUserId(@PathVariable Long userId) {
        checkOwnership(userId);
        return ResponseEntity.ok(matchingResultService.getByUserId(userId));
    }

    @GetMapping("/cv/{cvId}/best")
    public ResponseEntity<MatchingResultDTO> getBestByCvId(@PathVariable Long cvId) {
        checkOwnershipByCv(cvId);
        return ResponseEntity.ok(matchingResultService.getBestByCvId(cvId));
    }

    @PostMapping("/match")
    public ResponseEntity<MatchingResultDTO> matchAndSave(@RequestBody JobMatchRequestDTO request) {
        checkOwnershipByCv(request.getCvId());
        return ResponseEntity.ok(matchingResultService.matchAndSave(request));
    }
}