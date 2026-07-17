package com.projet.hirevisionai.Config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI hirevisionOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("HireVision AI — API Documentation")
                        .description("""
                            **HireVision AI** est une plateforme intelligente de préparation aux entretiens.
                            
                            ## Fonctionnalités principales
                            - 📄 **Analyse de CV** : extraction de compétences, profil IA, score global
                            - 🎯 **Job Matching** : compatibilité CV/offre avec détail des compétences
                            - 🎤 **Simulation d'entretien** : questions adaptatives, analyse vocale et comportementale
                            - 📊 **Developer Readiness Score** : score global multi-axe du développeur
                            - 🗺️ **Roadmap personnalisée** : plan de carrière hebdomadaire basé sur les lacunes
                            
                            ## Authentification
                            Utiliser le token JWT renvoyé par `/auth/login` dans le header `Authorization: Bearer <token>`.
                            """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("HireVision AI Team")
                                .email("hamdisafa235@gmail.com"))
                        .license(new License()
                                .name("Private — All Rights Reserved")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
