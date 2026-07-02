package com.dailytask.adapters.notifiers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailConfigurationTest {

    private Map<String, String> originalEnv;

    @BeforeEach
    void setUp() {
        // Store original environment (cannot truly modify, but we'll test builder)
        originalEnv = new HashMap<>();
    }

    @AfterEach
    void tearDown() {
        // Cleanup if needed
    }

    @Test
    void builder_withValidValues_buildsSuccessfully() {
        EmailConfiguration config = new EmailConfiguration.Builder()
            .smtpHost("smtp.gmail.com")
            .smtpPort(587)
            .username("test@gmail.com")
            .password("app-password")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .enableTls(true)
            .enableAuth(true)
            .timeoutMs(30000)
            .build();

        assertNotNull(config);
        assertEquals("smtp.gmail.com", config.getSmtpHost());
        assertEquals(587, config.getSmtpPort());
        assertEquals("test@gmail.com", config.getUsername());
        assertEquals("app-password", config.getPassword());
        assertEquals("from@example.com", config.getFromEmail());
        assertEquals("to@example.com", config.getToEmail());
        assertTrue(config.isEnableTls());
        assertTrue(config.isEnableAuth());
        assertEquals(30000, config.getTimeoutMs());
    }

    @Test
    void builder_withNullSmtpHost_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost(null)
                .smtpPort(587)
                .username("test@gmail.com")
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .build()
        );
    }

    @Test
    void builder_withBlankSmtpHost_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("   ")
                .smtpPort(587)
                .username("test@gmail.com")
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_SMTP_HOST"));
    }

    @Test
    void builder_withPortTooLow_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(0)
                .username("test@gmail.com")
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_SMTP_PORT"));
        assertTrue(exception.getMessage().contains("1 and 65535"));
    }

    @Test
    void builder_withPortTooHigh_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(65536)
                .username("test@gmail.com")
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_SMTP_PORT"));
    }

    @Test
    void builder_withValidPortBoundaries_succeeds() {
        // Test port 1 (lower boundary)
        EmailConfiguration config1 = new EmailConfiguration.Builder()
            .smtpHost("smtp.gmail.com")
            .smtpPort(1)
            .username("test@gmail.com")
            .password("password")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .build();
        assertEquals(1, config1.getSmtpPort());

        // Test port 65535 (upper boundary)
        EmailConfiguration config2 = new EmailConfiguration.Builder()
            .smtpHost("smtp.gmail.com")
            .smtpPort(65535)
            .username("test@gmail.com")
            .password("password")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .build();
        assertEquals(65535, config2.getSmtpPort());
    }

    @Test
    void builder_withNullUsername_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .username(null)
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .build()
        );
    }

    @Test
    void builder_withBlankUsername_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .username("")
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_USERNAME"));
    }

    @Test
    void builder_withNullPassword_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .username("test@gmail.com")
                .password(null)
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .build()
        );
    }

    @Test
    void builder_withBlankPassword_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .username("test@gmail.com")
                .password("  ")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_PASSWORD"));
    }

    @Test
    void builder_withInvalidFromEmail_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .username("test@gmail.com")
                .password("password")
                .fromEmail("invalid-email")
                .toEmail("to@example.com")
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_FROM"));
        assertTrue(exception.getMessage().contains("valid email address"));
    }

    @Test
    void builder_withInvalidToEmail_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .username("test@gmail.com")
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("not-an-email")
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_TO"));
        assertTrue(exception.getMessage().contains("valid email address"));
    }

    @Test
    void builder_withValidEmailFormats_succeeds() {
        EmailConfiguration config = new EmailConfiguration.Builder()
            .smtpHost("smtp.gmail.com")
            .smtpPort(587)
            .username("test@gmail.com")
            .password("password")
            .fromEmail("user.name+tag@example.co.uk")
            .toEmail("another_user@sub.domain.com")
            .build();

        assertNotNull(config);
        assertEquals("user.name+tag@example.co.uk", config.getFromEmail());
        assertEquals("another_user@sub.domain.com", config.getToEmail());
    }

    @Test
    void builder_withZeroTimeout_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .username("test@gmail.com")
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .timeoutMs(0)
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_TIMEOUT_MS"));
        assertTrue(exception.getMessage().contains("positive"));
    }

    @Test
    void builder_withNegativeTimeout_throwsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            new EmailConfiguration.Builder()
                .smtpHost("smtp.gmail.com")
                .smtpPort(587)
                .username("test@gmail.com")
                .password("password")
                .fromEmail("from@example.com")
                .toEmail("to@example.com")
                .timeoutMs(-1000)
                .build()
        );
        assertTrue(exception.getMessage().contains("EMAIL_TIMEOUT_MS"));
    }

    @Test
    void builder_withDefaultValues_appliesCorrectDefaults() {
        EmailConfiguration config = new EmailConfiguration.Builder()
            .smtpHost("smtp.gmail.com")
            .smtpPort(587)
            .username("test@gmail.com")
            .password("password")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            // Not setting optional values
            .build();

        assertTrue(config.isEnableTls());
        assertTrue(config.isEnableAuth());
        assertEquals(30000, config.getTimeoutMs());
    }

    @Test
    void builder_withTlsDisabled_succeeds() {
        EmailConfiguration config = new EmailConfiguration.Builder()
            .smtpHost("smtp.gmail.com")
            .smtpPort(25)
            .username("test@gmail.com")
            .password("password")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .enableTls(false)
            .build();

        assertFalse(config.isEnableTls());
    }

    @Test
    void builder_withAuthDisabled_succeeds() {
        EmailConfiguration config = new EmailConfiguration.Builder()
            .smtpHost("localhost")
            .smtpPort(25)
            .username("test@gmail.com")
            .password("password")
            .fromEmail("from@example.com")
            .toEmail("to@example.com")
            .enableAuth(false)
            .build();

        assertFalse(config.isEnableAuth());
    }
}
