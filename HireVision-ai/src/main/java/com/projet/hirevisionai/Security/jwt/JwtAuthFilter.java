package com.projet.hirevisionai.Security.jwt;


import com.projet.hirevisionai.Security.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // ⭐⭐⭐ CORRECTION CORS FINALE ⭐⭐⭐
        // Ignorer les requêtes preflight CORS (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Lire le header Authorization
        String authHeader = request.getHeader("Authorization");

        // 2. Si pas de token → continuer sans authentification (endpoints publics)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraire le token (supprimer "Bearer ")
        String token = authHeader.substring(7);
        String email;

        // 4. Extraire l'email depuis le token
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            // Token malformé → continuer sans authentification
            filterChain.doFilter(request, response);
            return;
        }

        // 5. Si email valide et pas encore authentifié
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            if (jwtService.isTokenValid(token, userDetails)) {
                var authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 6. Passer au filtre suivant
        filterChain.doFilter(request, response);
    }
}
