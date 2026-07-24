package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.UserDTO;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.PaymentRepository;
import com.projet.hirevisionai.Repository.SubscriptionRepository;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final org.springframework.web.client.RestTemplate restTemplate;

    @Value("${app.ai-service.url}")
    private String aiServiceUrl;

    @Override
    public UserDTO getById(Long id) {
        return UserDTO.fromEntity(
                userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("User introuvable : " + id)));
    }

    @Override
    public List<UserDTO> getAll() {
        return userRepository.findAll()
                .stream().map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO update(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User introuvable : " + id));
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        if (dto.getAge() > 0) user.setAge(dto.getAge());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getTitle() != null) user.setTitle(dto.getTitle());
        if (dto.getLinkedin() != null) user.setLinkedin(dto.getLinkedin());
        if (dto.getGithub() != null) user.setGithub(dto.getGithub());

        return UserDTO.fromEntity(userRepository.save(user));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!userRepository.existsById(id))
            throw new RuntimeException("User introuvable : " + id);

        // CV et Interview sont déjà en cascade (orphanRemoval) côté entité User,
        // mais Payment et Subscription référencent User sans cascade déclarée.
        // Sans ça, MySQL bloque le DELETE (contrainte de clé étrangère user_id)
        // dès que l'utilisateur a au moins un paiement ou un abonnement -> 500.
        // Ordre important : Payment référence aussi Subscription (subscription_id),
        // donc on supprime les paiements avant les abonnements.
        paymentRepository.deleteAll(paymentRepository.findByUserIdUser(id));
        subscriptionRepository.deleteAll(subscriptionRepository.findByUserIdUser(id));

        userRepository.deleteById(id);
    }

    // UserServiceImpl.java — implémenter
    @Override
    public UserDTO uploadPicture(Long id, MultipartFile file) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User introuvable : " + id));
        try {
            String filename = "user_" + id + "_" + System.currentTimeMillis()
                    + "_" + file.getOriginalFilename();
            Path uploadDir = Paths.get("uploads/pictures/");
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            user.setProfilePicture(filename);
            return UserDTO.fromEntity(userRepository.save(user));
        } catch (IOException e) {
            throw new RuntimeException("Erreur upload image : " + e.getMessage());
        }
    }

    @Override
    public Object analyzeGithub(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable : " + id));

        String githubUrl = user.getGithub();
        if (githubUrl == null || githubUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Veuillez renseigner votre profil GitHub dans vos paramètres.");
        }

        String username = githubUrl.trim();
        // Parse the username out of URLs like: https://github.com/username/repo or https://github.com/username
        if (username.contains("github.com/")) {
            username = username.substring(username.indexOf("github.com/") + 11);
        }
        if (username.contains("/")) {
            username = username.split("/")[0];
        }
        if (username.contains("?")) {
            username = username.split("\\?")[0];
        }

        if (username.isEmpty()) {
            throw new IllegalArgumentException("Nom d'utilisateur GitHub invalide.");
        }

        String url = aiServiceUrl + "/analyze-github?username=" + username;
        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Le service d'analyse GitHub est temporairement indisponible : " + e.getMessage());
        }
    }
}