package com.dailytask.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.dailytask.core.config.AppConfig;
import com.dailytask.core.ports.DataSource;
import com.dailytask.core.ports.TaskExtractor;
import com.dailytask.core.ports.TaskNotifier;
import com.dailytask.core.ports.TaskSummarizer;
import com.dailytask.core.usecases.DailyTaskOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * AWS Lambda handler for daily task orchestration.
 *
 * <p>Invoked by EventBridge scheduled events (cron trigger at 9:00 AM daily).
 * Reuses existing AppConfig factories to maintain consistency with local execution.
 *
 * <p>Environment Requirements:
 * <ul>
 *   <li>DEPLOYMENT_ENV=lambda (selects AWS Secrets Manager for config)</li>
 *   <li>AWS_REGION (required for Secrets Manager and Bedrock access)</li>
 * </ul>
 *
 * <p>IAM Permissions Required:
 * <ul>
 *   <li>secretsmanager:GetSecretValue</li>
 *   <li>secretsmanager:UpdateSecret (for OAuth token refresh)</li>
 *   <li>bedrock:InvokeModel</li>
 *   <li>logs:CreateLogGroup, logs:CreateLogStream, logs:PutLogEvents</li>
 * </ul>
 */
public class DailyTaskLambdaHandler implements RequestHandler<ScheduledEvent, String> {
    private static final Logger logger = LoggerFactory.getLogger(DailyTaskLambdaHandler.class);

    /**
     * Handles scheduled EventBridge invocations.
     *
     * @param event EventBridge scheduled event containing trigger metadata
     * @param context Lambda execution context (request ID, remaining time, etc.)
     * @return "SUCCESS" if orchestration completed, throws RuntimeException otherwise
     */
    @Override
    public String handleRequest(ScheduledEvent event, Context context) {
        logger.info("=== Lambda Invoked by EventBridge ===");
        logger.info("Event ID: {}", event.getId());
        logger.info("Request ID: {}", context.getAwsRequestId());
        logger.info("Function Name: {}", context.getFunctionName());
        logger.info("Remaining Time: {} ms", context.getRemainingTimeInMillis());

        try {
            // Reuse existing AppConfig factories - ZERO code duplication
            logger.info("Initializing components via AppConfig factories...");

            List<DataSource> sources = AppConfig.createDataSources();
            logger.info("Created {} data source(s)", sources.size());

            TaskExtractor extractor = AppConfig.createTaskExtractor();
            logger.info("Created task extractor");

            TaskSummarizer analyzer = AppConfig.createAnalyzer();
            logger.info("Created task summarizer (Claude integration)");

            TaskNotifier notifier = AppConfig.createNotifier();
            logger.info("Created task notifier (email)");

            DailyTaskOrchestrator orchestrator = new DailyTaskOrchestrator(
                sources, extractor, analyzer, notifier
            );

            logger.info("Starting orchestration...");
            long startTime = System.currentTimeMillis();

            orchestrator.execute();

            long duration = System.currentTimeMillis() - startTime;
            logger.info("Orchestration completed successfully in {} ms", duration);
            logger.info("Remaining time after execution: {} ms", context.getRemainingTimeInMillis());

            return "SUCCESS";

        } catch (Exception e) {
            logger.error("Orchestration failed", e);
            logger.error("Request ID: {}", context.getAwsRequestId());
            logger.error("Function Name: {}", context.getFunctionName());

            // Throw to mark Lambda execution as failed (appears in CloudWatch metrics)
            throw new RuntimeException("Orchestration failed: " + e.getMessage(), e);
        }
    }
}
