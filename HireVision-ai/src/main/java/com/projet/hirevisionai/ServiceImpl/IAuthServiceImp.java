package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.*;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.Security.CustomUserDetailsService;
import com.projet.hirevisionai.Security.jwt.JwtService;
import com.projet.hirevisionai.ServiceInterface.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IAuthServiceImp implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
   // private final PromoEngineService promoEngineService;


    private static final String FRONTEND_URL = "http://localhost:4200";
    private final EmailService emailService;



    @Override
    public User register(RegisterRequest req) {

        // Validation des champs
        if (req.email() == null || req.email().isBlank()) {
            throw new IllegalArgumentException("Email required");
        }

        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("Email already used");
        }

        if (req.password() == null || req.password().length() < 6) {
            throw new IllegalArgumentException("Password must contain at least 6 characters");
        }

        if (req.role() == null) {
            throw new IllegalArgumentException("Role required");
        }

        // Création user
        User u = User.builder()
                .fullName(req.fullName() == null ? "Not Available" : req.fullName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .role(req.role() != null ? req.role() : Role.CANDIDATE)  // ← défaut CANDIDATE
                .age(req.age())
                .enabled(true)
                .build();

        // 🔥 SAVE USER
        User savedUser = userRepository.save(u);

        // 🔥 TRIGGER PROMO ENGINE ICI (IMPORTANT)
        //promoEngineService.generateWelcomePromo(savedUser);

        return savedUser;
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        // Spring Security vérifie email + mot de passe (lève une exception si invalide)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(req.email());

        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No roles found"))
                .getAuthority();

        User user = userRepository.findByEmail(req.email()).orElseThrow();
        String token = jwtService.generateToken(user);


        return new AuthResponse(token, userDetails.getUsername(), role, user.getIdUser());
    }



    @Override
    public AuthResponse completeGoogleRegister(CompleteGoogleRegisterRequest req) {

        // Vérifier que l'utilisateur n'existe pas déjà
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new IllegalArgumentException("Email already used");
        }

        // Créer l'utilisateur avec le rôle choisi
        User user = User.builder()
                .email(req.email())
                .fullName(req.fullName())
                .password("GOOGLE_OAUTH2_NO_PASSWORD")
                .role(req.role())
                .enabled(true)
                .build();

        userRepository.save(user);

        // Générer le JWT
        var authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
        var userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();

        String token = jwtService.generateToken(user);

        return new AuthResponse(token, user.getEmail(), "ROLE_" + user.getRole().name(), user.getIdUser());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest req) {
        Optional<User> optUser = userRepository.findByEmail(req.email());
        if (optUser.isEmpty()) return;

        User user = optUser.get();

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
        userRepository.save(user);

        String resetLink = FRONTEND_URL + "/reset-password?token=" + token;  // ✅ ici

        try {
            emailService.sendResetEmail(user.getEmail(), resetLink);
        } catch (Exception e) {
            System.out.println("⚠️ Email non envoyé: " + e.getMessage());
            System.out.println(">>> RESET LINK: " + resetLink);
        }
    }

    @Override
    public void resetPassword(ResetPasswordRequest req) {
        User user = userRepository.findByResetToken(req.token())
                .orElseThrow(() -> new IllegalArgumentException("Token invalide"));

        if (user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token expiré");
        }

        if (req.newPassword() == null || req.newPassword().length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }



}
