package com.gbm.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender, @Value("${app.mail.from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendOtpEmail(String email, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                message.setFrom(fromAddress);
            }
            message.setTo(email);
            message.setSubject("MyGarage password reset OTP");
            message.setText("Your MyGarage OTP is " + otp + ". It expires in 10 minutes.");
            mailSender.send(message);
        } catch (Exception ex) {
            log.warn("Failed to send OTP email to {}. Falling back to server log only.", email, ex);
            log.info("MyGarage OTP for {} => {}", email, otp);
        }
    }
}
