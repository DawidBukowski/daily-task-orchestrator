package com.dailytask.adapters.notifiers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SmtpEmailSenderTest {

    private EmailConfiguration config;

    @BeforeEach
    void setUp() {
        config = EmailConfiguration.builder()
            .smtpHost("smtp.gmail.com")
            .smtpPort(587)
            .username("test@example.com")
            .password("password123")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .build();
    }

    @Test
    void constructor_withNullConfig_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new SmtpEmailSender(null));
    }

    @Test
    void constructor_withValidConfig_succeeds() {
        assertDoesNotThrow(() -> new SmtpEmailSender(config));
    }

    @Test
    void send_withNullSubject_throwsNullPointerException() {
        SmtpEmailSender sender = new SmtpEmailSender(config);
        assertThrows(NullPointerException.class, () -> sender.send(null, "<html>Body</html>"));
    }

    @Test
    void send_withNullBody_throwsNullPointerException() {
        SmtpEmailSender sender = new SmtpEmailSender(config);
        assertThrows(NullPointerException.class, () -> sender.send("Subject", null));
    }

    @Test
    void send_withInvalidSmtpHost_throwsEmailSendException() {
        EmailConfiguration invalidConfig = EmailConfiguration.builder()
            .smtpHost("invalid.smtp.host.example.com")
            .smtpPort(587)
            .username("test@example.com")
            .password("wrongpassword")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .timeoutMs(5000)
            .build();

        SmtpEmailSender sender = new SmtpEmailSender(invalidConfig);

        assertThrows(SmtpEmailSender.EmailSendException.class, () ->
            sender.send("Test Subject", "<html><body>Test</body></html>"));
    }
}
