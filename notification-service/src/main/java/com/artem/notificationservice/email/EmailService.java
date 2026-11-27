package com.artem.notificationservice.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${app.mail.from}")
    private String from;

    public void sendRegistrationEmail(
            String recipientEmail,
            String subject,
            String message
    ) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();

        mailMessage.setFrom(from);
        mailMessage.setTo(recipientEmail);
        mailMessage.setSubject(subject);
        mailMessage.setText(message);

        javaMailSender.send(mailMessage);
    }
}