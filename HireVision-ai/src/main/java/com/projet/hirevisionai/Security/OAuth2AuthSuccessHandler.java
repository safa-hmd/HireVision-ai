package com.projet.hirevisionai.Security;


import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.Security.jwt.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@RequiredArgsConstructor
public class OAuth2AuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email    = oAuth2User.getAttribute("email");
        String fullName = oAuth2User.getAttribute("name");

        var existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User user = existingUser.get();
            String token = generateJwt(user);

            String redirectUrl = String.format(
                    "http://localhost:4200/oauth2/callback?token=%s&email=%s&role=ROLE_%s&id=%d",
                    token, user.getEmail(), user.getRole().name(), user.getIdUser()
            );
            response.sendRedirect(redirectUrl);

        } else {
            String redirectUrl = String.format(
                    "http://localhost:4200/oauth2/select-role?email=%s&name=%s",
                    email, fullName != null ? fullName : "Google User"
            );
            response.sendRedirect(redirectUrl);
        }
    }

    private String generateJwt(User user) {
        return jwtService.generateToken(user);
    }
}
