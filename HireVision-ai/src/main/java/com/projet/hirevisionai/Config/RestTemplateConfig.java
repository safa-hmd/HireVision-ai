package com.projet.hirevisionai.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        // Sans timeout explicite, SimpleClientHttpRequestFactory attend
        // indéfiniment une réponse (readTimeout = -1 par défaut). Si Python/
        // Gemini est lent ou bloqué, la requête Angular reste alors bloquée
        // sur le spinner de chargement pour toujours. On borne donc les délais.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000); // 10s pour établir la connexion à Python
        factory.setReadTimeout(60_000);    // 60s pour laisser le temps à Gemini de répondre
        return new RestTemplate(factory);
    }
}