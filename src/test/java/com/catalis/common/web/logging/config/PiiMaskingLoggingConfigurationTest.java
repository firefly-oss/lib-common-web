package com.catalis.common.web.logging.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.catalis.common.web.logging.appender.PiiMaskingAppender;
import com.catalis.common.web.logging.service.PiiMaskingService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test class for PiiMaskingLoggingConfiguration to verify automatic PII masking
 * is properly configured for all application logs.
 */
@ExtendWith(MockitoExtension.class)
public class PiiMaskingLoggingConfigurationTest {

    @Mock
    private ApplicationReadyEvent applicationReadyEvent;

    private PiiMaskingProperties properties;
    private PiiMaskingService piiMaskingService;
    private PiiMaskingLoggingConfiguration loggingConfiguration;
    private LoggerContext loggerContext;
    private Logger rootLogger;
    private ListAppender<ILoggingEvent> testAppender;

    @BeforeEach
    void setUp() {
        // Setup properties
        properties = new PiiMaskingProperties();
        properties.setEnabled(true);
        properties.setAutoMaskLogs(true);

        // Setup PII masking service
        piiMaskingService = new PiiMaskingService(properties);

        // Setup logging configuration
        loggingConfiguration = new PiiMaskingLoggingConfiguration(piiMaskingService, properties);

        // Setup Logback context
        loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);

        // Create a test appender to capture log messages
        testAppender = new ListAppender<>();
        testAppender.setContext(loggerContext);
        testAppender.start();

        // Add test appender to root logger
        rootLogger.addAppender(testAppender);
    }

    @AfterEach
    void tearDown() {
        // Clean up appenders
        rootLogger.detachAndStopAllAppenders();
        testAppender.stop();
    }

    @Test
    void testPiiMaskingConfigurationEnabled() {
        // Given: PII masking is enabled
        assertTrue(properties.isEnabled());
        assertTrue(properties.isAutoMaskLogs());

        // When: Application ready event is fired
        loggingConfiguration.onApplicationEvent(applicationReadyEvent);

        // Then: Root logger should have PiiMaskingAppender
        boolean hasPiiMaskingAppender = false;
        var appenderIterator = rootLogger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            if (appenderIterator.next() instanceof PiiMaskingAppender) {
                hasPiiMaskingAppender = true;
                break;
            }
        }
        assertTrue(hasPiiMaskingAppender, "Root logger should have PiiMaskingAppender");
    }

    @Test
    void testPiiMaskingConfigurationDisabled() {
        // Given: PII masking is disabled
        properties.setAutoMaskLogs(false);

        // When: Application ready event is fired
        loggingConfiguration.onApplicationEvent(applicationReadyEvent);

        // Then: Root logger should not have PiiMaskingAppender
        boolean hasPiiMaskingAppender = false;
        var appenderIterator = rootLogger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            if (appenderIterator.next() instanceof PiiMaskingAppender) {
                hasPiiMaskingAppender = true;
                break;
            }
        }
        assertFalse(hasPiiMaskingAppender, "Root logger should not have PiiMaskingAppender when disabled");
    }

    @Test
    void testAutomaticLogMasking() {
        // Given: PII masking is configured (this wraps existing appenders including testAppender)
        loggingConfiguration.onApplicationEvent(applicationReadyEvent);

        // Clear any existing events from setup
        testAppender.list.clear();

        // When: Log a message with PII data
        Logger testLogger = loggerContext.getLogger("test.logger");
        String messageWithPii = "User email is john.doe@example.com and phone is 555-123-4567";
        testLogger.info(messageWithPii);

        // Then: The wrapped testAppender should receive the masked events
        List<ILoggingEvent> logEvents = testAppender.list;
        assertFalse(logEvents.isEmpty(), "Should have captured log events");

        ILoggingEvent logEvent = logEvents.get(0);
        String formattedMessage = logEvent.getFormattedMessage();
        
        System.out.println("[DEBUG_LOG] Original message: " + messageWithPii);
        System.out.println("[DEBUG_LOG] Formatted message: " + formattedMessage);
        System.out.println("[DEBUG_LOG] PII masking service ready: " + piiMaskingService.isReady());
        
        // Debug: Test the service directly
        String directMasked = piiMaskingService.maskPiiData(messageWithPii);
        System.out.println("[DEBUG_LOG] Direct service masking: " + directMasked);

        // The message should be different from original (masked)
        assertNotEquals(messageWithPii, formattedMessage, "Message should be masked");
        
        // Should not contain the original PII data
        assertFalse(formattedMessage.contains("john.doe@example.com"), "Should not contain original email");
        assertFalse(formattedMessage.contains("555-123-4567"), "Should not contain original phone");
        
        // Should still contain the non-PII parts
        assertTrue(formattedMessage.contains("User email is"), "Should contain non-PII text");
        assertTrue(formattedMessage.contains("and phone is"), "Should contain non-PII text");
    }

    @Test
    void testMaskingServiceDisabled() {
        // Given: PII masking service is disabled
        properties.setEnabled(false);

        // When: Application ready event is fired
        loggingConfiguration.onApplicationEvent(applicationReadyEvent);

        // Then: Should not setup PII masking
        boolean hasPiiMaskingAppender = false;
        var appenderIterator = rootLogger.iteratorForAppenders();
        while (appenderIterator.hasNext()) {
            if (appenderIterator.next() instanceof PiiMaskingAppender) {
                hasPiiMaskingAppender = true;
                break;
            }
        }
        assertFalse(hasPiiMaskingAppender, "Should not have PiiMaskingAppender when service is disabled");
    }

    @Test
    void testErrorHandlingDoesNotBreakLogging() {
        // Given: A configuration that might throw an exception (simulated by null service)
        PiiMaskingLoggingConfiguration faultyConfiguration = 
            new PiiMaskingLoggingConfiguration(null, properties);

        // When: Application ready event is fired
        // Then: Should not throw exception
        assertDoesNotThrow(() -> {
            faultyConfiguration.onApplicationEvent(applicationReadyEvent);
        }, "Error in PII masking setup should not break application startup");

        // And logging should still work
        Logger testLogger = loggerContext.getLogger("test.logger");
        assertDoesNotThrow(() -> {
            testLogger.info("This should still work");
        }, "Logging should still work even if PII masking setup fails");
    }
}