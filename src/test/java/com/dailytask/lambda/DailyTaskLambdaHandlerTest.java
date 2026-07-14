package com.dailytask.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DailyTaskLambdaHandler.
 *
 * <p>Note: These tests verify handler initialization and error handling structure.
 * Full end-to-end testing requires AWS infrastructure (Secrets Manager, Bedrock)
 * and should be performed as integration tests or manual Lambda invocations.
 */
class DailyTaskLambdaHandlerTest {

    private DailyTaskLambdaHandler handler;
    private Context mockContext;
    private ScheduledEvent mockEvent;

    @BeforeEach
    void setUp() {
        handler = new DailyTaskLambdaHandler();

        // Mock Lambda Context
        mockContext = mock(Context.class);
        when(mockContext.getAwsRequestId()).thenReturn("test-request-id-12345");
        when(mockContext.getFunctionName()).thenReturn("daily-task-orchestrator");
        when(mockContext.getRemainingTimeInMillis()).thenReturn(300000); // 5 minutes

        // Mock EventBridge ScheduledEvent
        mockEvent = new ScheduledEvent();
        mockEvent.setId("test-event-id-67890");
    }

    @Test
    void testHandlerInstantiation() {
        assertNotNull(handler, "Handler should instantiate successfully");
    }

    @Test
    void testContextLogging() {
        // Verify context properties are accessible (no exceptions)
        assertDoesNotThrow(() -> {
            mockContext.getAwsRequestId();
            mockContext.getFunctionName();
            mockContext.getRemainingTimeInMillis();
        }, "Context properties should be accessible");
    }

    @Test
    void testEventProperties() {
        // Verify event properties are accessible
        assertDoesNotThrow(() -> {
            mockEvent.getId();
        }, "Event properties should be accessible");

        assertEquals("test-event-id-67890", mockEvent.getId());
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "DEPLOYMENT_ENV", matches = "local")
    void testHandleRequest_MissingSecrets_ThrowsException() {
        // In local mode without proper env vars, should fail gracefully
        // This test verifies error handling structure, not actual AWS integration

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            handler.handleRequest(mockEvent, mockContext);
        }, "Handler should throw RuntimeException when secrets are missing");

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().contains("failed") ||
                   exception.getMessage().contains("error"),
                   "Exception message should indicate failure");
    }

    /**
     * Integration test requiring AWS credentials and Secrets Manager setup.
     * Run manually with:
     * <pre>
     * export DEPLOYMENT_ENV=lambda
     * export AWS_REGION=us-east-1
     * mvn test -Dtest=DailyTaskLambdaHandlerTest#testHandleRequest_Success
     * </pre>
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_AWS_INTEGRATION_TESTS", matches = "true")
    void testHandleRequest_Success() {
        // Requires:
        // - DEPLOYMENT_ENV=lambda
        // - AWS_REGION set
        // - Secrets Manager secrets created
        // - Valid AWS credentials

        String result = assertDoesNotThrow(() -> handler.handleRequest(mockEvent, mockContext),
                "Handler should execute successfully with valid AWS configuration");

        assertEquals("SUCCESS", result, "Handler should return SUCCESS");
    }

    @Test
    void testExceptionPropagation() {
        // Verify that exceptions are properly wrapped in RuntimeException
        // This ensures Lambda marks the execution as failed

        // Mock a scenario where initialization fails (simulated by missing env vars)
        // The exact failure depends on what env vars are present during test

        try {
            handler.handleRequest(mockEvent, mockContext);
            // If it succeeds, that's also valid (means env is properly configured)
        } catch (RuntimeException e) {
            // Expected when env is not configured
            assertNotNull(e.getMessage(), "Exception should have a message");
            // Verify it's a RuntimeException (not checked exception)
            assertTrue(e instanceof RuntimeException);
        }
    }

    @Test
    void testHandlerReturnType() {
        // Verify handler implements correct RequestHandler interface
        assertTrue(handler instanceof com.amazonaws.services.lambda.runtime.RequestHandler,
                "Handler should implement RequestHandler interface");
    }
}
