package com.dailytask.adapters.analyzers;

import com.dailytask.core.config.ClaudeConfiguration;
import com.dailytask.core.ports.ClaudeApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

import java.util.List;

/**
 * AWS Bedrock Claude client implementation using AWS SDK v2.
 * Uses the Bedrock Converse API to communicate with Claude models.
 */
public class AwsBedrockClaudeClient implements ClaudeApiClient {
    private static final Logger logger = LoggerFactory.getLogger(AwsBedrockClaudeClient.class);

    private final ClaudeConfiguration config;
    private final BedrockRuntimeClient bedrockClient;

    public AwsBedrockClaudeClient(ClaudeConfiguration config) {
        this.config = config;
        this.bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.of(config.getAwsRegion()))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }

    @Override
    public String sendMessage(String systemPrompt, String userPrompt) throws ClaudeApiClient.ClaudeApiException {
        try {
            logger.debug("Sending request to AWS Bedrock (region: {}, model: {})",
                config.getAwsRegion(), config.getModelId());

            // Build the request using Converse API
            ConverseRequest.Builder requestBuilder = ConverseRequest.builder()
                .modelId(config.getModelId());

            // Add system prompt if provided
            if (systemPrompt != null && !systemPrompt.isBlank()) {
                SystemContentBlock systemBlock = SystemContentBlock.builder()
                    .text(systemPrompt)
                    .build();
                requestBuilder.system(List.of(systemBlock));
            }

            // Build user message
            Message userMessage = Message.builder()
                .role(ConversationRole.USER)
                .content(ContentBlock.fromText(userPrompt))
                .build();
            requestBuilder.messages(List.of(userMessage));

            // Set inference configuration
            InferenceConfiguration inferenceConfig = InferenceConfiguration.builder()
                .maxTokens(config.getMaxTokens())
                .temperature((float) config.getTemperature())
                .build();
            requestBuilder.inferenceConfig(inferenceConfig);

            // Execute the request
            ConverseRequest request = requestBuilder.build();
            ConverseResponse response = bedrockClient.converse(request);

            return extractTextFromResponse(response);

        } catch (ThrottlingException e) {
            logger.warn("Rate limit exceeded on AWS Bedrock", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "Rate limit exceeded. Please try again later.",
                ClaudeApiClient.ClaudeApiException.ErrorType.RATE_LIMIT_EXCEEDED,
                e
            );
        } catch (AccessDeniedException e) {
            logger.error("AWS authentication/authorization failed", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "AWS authentication failed. Please check your AWS credentials and permissions.",
                ClaudeApiClient.ClaudeApiException.ErrorType.AUTHENTICATION_FAILED,
                e
            );
        } catch (ValidationException e) {
            logger.error("Invalid request to AWS Bedrock", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "Invalid request: " + e.getMessage(),
                ClaudeApiClient.ClaudeApiException.ErrorType.INVALID_REQUEST,
                e
            );
        } catch (InternalServerException | ServiceQuotaExceededException e) {
            logger.error("AWS Bedrock server error", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "AWS Bedrock server error: " + e.getMessage(),
                ClaudeApiClient.ClaudeApiException.ErrorType.SERVER_ERROR,
                e
            );
        } catch (ModelTimeoutException e) {
            logger.error("Model inference timed out", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "Model inference timed out",
                ClaudeApiClient.ClaudeApiException.ErrorType.TIMEOUT,
                e
            );
        } catch (SdkException e) {
            logger.error("AWS SDK error", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "AWS SDK error: " + e.getMessage(),
                ClaudeApiClient.ClaudeApiException.ErrorType.NETWORK_ERROR,
                e
            );
        } catch (Exception e) {
            logger.error("Unexpected error calling AWS Bedrock", e);
            throw new ClaudeApiClient.ClaudeApiException(
                "Unexpected error: " + e.getMessage(),
                ClaudeApiClient.ClaudeApiException.ErrorType.NETWORK_ERROR,
                e
            );
        }
    }

    private String extractTextFromResponse(ConverseResponse response) throws ClaudeApiClient.ClaudeApiException {
        if (response.output() == null) {
            throw new ClaudeApiClient.ClaudeApiException(
                "Response does not contain output",
                ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE
            );
        }

        Message outputMessage = response.output().message();
        if (outputMessage == null || outputMessage.content() == null || outputMessage.content().isEmpty()) {
            throw new ClaudeApiClient.ClaudeApiException(
                "Response message does not contain content",
                ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE
            );
        }

        // Extract text from the first content block
        ContentBlock firstBlock = outputMessage.content().get(0);
        String text = firstBlock.text();

        if (text == null || text.isBlank()) {
            throw new ClaudeApiClient.ClaudeApiException(
                "Content block does not contain text",
                ClaudeApiClient.ClaudeApiException.ErrorType.MALFORMED_RESPONSE
            );
        }

        logger.debug("Successfully extracted response text ({} characters)", text.length());
        logger.debug("Token usage - input: {}, output: {}",
            response.usage().inputTokens(),
            response.usage().outputTokens());

        return text;
    }

    /**
     * Closes the Bedrock client and releases resources.
     */
    public void close() {
        if (bedrockClient != null) {
            bedrockClient.close();
        }
    }
}
