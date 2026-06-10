package com.projet.hirevisionai.ServiceImpl;

import lombok.RequiredArgsConstructor;


import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("StreetLeague — Réinitialisation de mot de passe");
        message.setText(
                "Bonjour,\n\n" +
                        "Vous avez demandé à réinitialiser votre mot de passe StreetLeague.\n\n" +
                        "Cliquez sur le lien ci-dessous (valable 30 minutes) :\n" +
                        resetLink + "\n\n" +
                        "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n" +
                        "— L'équipe StreetLeague"
        );
        mailSender.send(message);
    }

    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = HTML

            mailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            e.printStackTrace();
            System.err.println("Échec de l'envoi de l'email HTML à " + to);
        }
    }
}