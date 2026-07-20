package com.projet.hirevisionai.ServiceImpl;

import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendResetEmailTest_shouldBuildAndSendMessage_withResetLink() {
        emailService.sendResetEmail("jean@test.com", "https://hirevision.ai/reset?token=abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).contains("jean@test.com");
        assertThat(sent.getSubject()).contains("Réinitialisation");
        assertThat(sent.getText()).contains("https://hirevision.ai/reset?token=abc");
    }

    @Test
    void sendHtmlEmailTest_shouldSendMimeMessage_whenValid() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((jakarta.mail.Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("jean@test.com", "Sujet", "<p>Bonjour</p>");

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void sendHtmlEmailTest_shouldPropagate_whenMailSenderThrowsRuntimeException() {
        // Une RuntimeException (ex: MailSendException) levée par send() n'est PAS catchée
        // par le bloc try/catch (qui ne capture que MessagingException) : elle doit se propager.
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new org.springframework.mail.MailSendException("Erreur SMTP"))
                .when(mailSender).send(any(MimeMessage.class));

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.mail.MailSendException.class,
                () -> emailService.sendHtmlEmail("jean@test.com", "Sujet", "<p>Bonjour</p>")
        );
    }
}
