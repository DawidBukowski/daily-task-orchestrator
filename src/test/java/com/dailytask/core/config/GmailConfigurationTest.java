package com.dailytask.core.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GmailConfigurationTest {

    @Test
    void constructor_ThrowsException_WhenCredentialsMissing() {
        assertThrows(IllegalStateException.class, () ->
                new GmailConfiguration("", "", "http://localhost", "~/.tokens", List.of())
        );
    }

    @Test
    void constructor_Succeeds_WithValidData() {
        GmailConfiguration config = new GmailConfiguration("id", "secret", "http://local", "~/.tokens", List.of("scope1"));
        assertEquals("id", config.getClientId());

        String expectedPath = System.getProperty("user.home") + "/.tokens";
        assertEquals(expectedPath, config.getTokenDirectory());
    }
}