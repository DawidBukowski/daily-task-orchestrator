package com.dailytask.adapters.notifiers;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;
import java.util.Properties;

/**
 * SMTP email sender using Jakarta Mail API.
 * Handles authentication, TLS configuration, and HTML email sending.
 */
public class SmtpEmailSender {
    private static final Logger logger = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final EmailConfiguration config;

    public SmtpEmailSender(EmailConfiguration config) {
        this.config = Objects.requireNonNull(config, "EmailConfiguration cannot be null");
    }

    /**
     * Sends an HTML email via SMTP.
     *
     * @param subject email subject line
     * @param htmlBody HTML content
     * @throws EmailSendException if sending fails
     */
    public void send(String subject, String htmlBody) throws EmailSendException {
        Objects.requireNonNull(subject, "Subject cannot be null");
        Objects.requireNonNull(htmlBody, "HTML body cannot be null");

        try {
            logger.debug("Creating SMTP session for {}:{}", config.getSmtpHost(), config.getSmtpPort());
            Session session = createSession();

            logger.debug("Creating email message: from={}, to={}", config.getFromEmail(), config.getToEmail());
            MimeMessage message = createMessage(session, subject, htmlBody);

            logger.debug("Sending email via SMTP...");
            Transport.send(message);

            logger.info("Email sent successfully to {}", config.getToEmail());

        } catch (MessagingException e) {
            logger.error("SMTP send failed: {}", e.getMessage());
            throw new EmailSendException("Failed to send email via SMTP", e);
        }
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", config.getSmtpHost());
        props.put("mail.smtp.port", String.valueOf(config.getSmtpPort()));
        props.put("mail.smtp.starttls.enable", String.valueOf(config.isEnableTls()));
        props.put("mail.smtp.auth", String.valueOf(config.isEnableAuth()));
        props.put("mail.smtp.timeout", String.valueOf(config.getTimeoutMs()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(config.getTimeoutMs()));
        props.put("mail.smtp.writetimeout", String.valueOf(config.getTimeoutMs()));

        Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getUsername(), config.getPassword());
            }
        };

        return Session.getInstance(props, authenticator);
    }

    private MimeMessage createMessage(Session session, String subject, String htmlBody)
            throws MessagingException {
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getFromEmail()));
        message.setRecipients(
            Message.RecipientType.TO,
            InternetAddress.parse(config.getToEmail()));
        message.setSubject(subject, "UTF-8");
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        message.setSentDate(new Date());
        return message;
    }

    /**
     * Exception thrown when email sending fails.
     */
    public static class EmailSendException extends Exception {
        public EmailSendException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
