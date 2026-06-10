package com.projet.hirevisionai.Security;

import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    // username = email dans notre application
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        // Récupérer l'utilisateur depuis la BD
        User u = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        // Convertir le rôle applicatif en autorité Spring Security (préfixe ROLE_ obligatoire)
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()));

        // Construire et retourner l'objet UserDetails utilisé par Spring Security
        return org.springframework.security.core.userdetails.User
                .withUsername(u.getEmail())
                .password(u.getPassword())
                .authorities(authorities)
                .disabled(!u.isEnabled())
                .build();
    }

}
