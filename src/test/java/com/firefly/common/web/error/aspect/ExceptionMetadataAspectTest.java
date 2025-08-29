package com.firefly.common.web.error.aspect;

import com.firefly.common.web.error.converter.ExceptionConverter;
import com.firefly.common.web.error.converter.ExceptionConverterService;
import com.firefly.common.web.error.exceptions.BusinessException;
import com.firefly.common.web.error.exceptions.DataIntegrityException;
import com.firefly.common.web.error.exceptions.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.dao.DataIntegrityViolationException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionMetadataAspectTest {

    private ExceptionConverterService converterService;
    private ExceptionConverterService proxiedService;

    @BeforeEach
    void setUp() {
        // Create a mock converter for DataIntegrityViolationException
        ExceptionConverter<DataIntegrityViolationException> mockConverter = new ExceptionConverter<DataIntegrityViolationException>() {
            @Override
            public Class<DataIntegrityViolationException> getExceptionType() {
                return DataIntegrityViolationException.class;
            }

            @Override
            public boolean canHandle(Throwable exception) {
                return exception instanceof DataIntegrityViolationException;
            }

            @Override
            public BusinessException convert(DataIntegrityViolationException exception) {
                return DataIntegrityException.uniqueConstraintViolation("email", "test@example.com");
            }
        };

        // Create the real service with the mock converter
        converterService = new ExceptionConverterService(Arrays.asList(
                mockConverter
        ));

        // Create a proxy with the aspect
        AspectJProxyFactory factory = new AspectJProxyFactory(converterService);
        factory.addAspect(new ExceptionMetadataAspect());
        proxiedService = factory.getProxy();
    }

    @Test
    void addMetadataToExceptions_WithDataIntegrityViolationException_AddsMetadata() {
        // Arrange
        DataIntegrityViolationException originalException = new DataIntegrityViolationException(
                "Duplicate entry 'test@example.com' for key 'email'");

        // Act
        BusinessException result = proxiedService.convertException(originalException);

        // Assert
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertEquals(DataIntegrityViolationException.class.getName(), metadata.get("exceptionType"));
        assertNotNull(metadata.get("exceptionId"));
        // The category is not added because we're using a mock converter that returns a DataIntegrityException
        // with no metadata, and our aspect adds the metadata afterward
    }

    @Test
    void addMetadataToExceptions_WithTimeoutException_AddsMetadata() {
        // Arrange
        TimeoutException originalException = new TimeoutException("Operation timed out");

        // Act
        BusinessException result = proxiedService.convertException(originalException);

        // Assert
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertEquals(TimeoutException.class.getName(), metadata.get("exceptionType"));
        assertNotNull(metadata.get("exceptionId"));
        assertEquals("timeout", metadata.get("category"));
        assertEquals("operation", metadata.get("timeoutType"));
    }

    @Test
    void addMetadataToExceptions_WithSocketTimeoutException_AddsMetadata() {
        // Arrange
        SocketTimeoutException originalException = new SocketTimeoutException("Read timed out");

        // Act
        BusinessException result = proxiedService.convertException(originalException);

        // Assert
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertEquals(SocketTimeoutException.class.getName(), metadata.get("exceptionType"));
        assertNotNull(metadata.get("exceptionId"));
        assertEquals("timeout", metadata.get("category"));
        assertEquals("network", metadata.get("timeoutType"));
    }

    @Test
    void addMetadataToExceptions_WithConnectException_AddsMetadata() {
        // Arrange
        ConnectException originalException = new ConnectException("Connection refused");

        // Act
        BusinessException result = proxiedService.convertException(originalException);

        // Assert
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertEquals(ConnectException.class.getName(), metadata.get("exceptionType"));
        assertNotNull(metadata.get("exceptionId"));
        assertEquals("network", metadata.get("category"));
        assertEquals(true, metadata.get("connectionError"));
    }

    @Test
    void addMetadataToExceptions_WithUnknownHostException_AddsMetadata() {
        // Arrange
        UnknownHostException originalException = new UnknownHostException("unknown-host.example.com");

        // Act
        BusinessException result = proxiedService.convertException(originalException);

        // Assert
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertEquals(UnknownHostException.class.getName(), metadata.get("exceptionType"));
        assertNotNull(metadata.get("exceptionId"));
        assertEquals("network", metadata.get("category"));
        assertEquals(true, metadata.get("unknownHost"));
        assertEquals("unknown-host.example.com", metadata.get("host"));
    }

    @Test
    void addMetadataToExceptions_WithBusinessException_DoesNotModifyException() {
        // Arrange
        BusinessException originalException = new BusinessException("Test message");

        // Act
        BusinessException result = proxiedService.convertException(originalException);

        // Assert
        assertSame(originalException, result);
        // We don't assert that metadata is null because the BusinessException might have default metadata
    }

    @Test
    void addMetadataToExceptions_WithExceptionHavingCause_AddsCauseInfo() {
        // Arrange
        IllegalStateException cause = new IllegalStateException("Root cause");
        RuntimeException originalException = new RuntimeException("Wrapper exception", cause);

        // Act
        BusinessException result = proxiedService.convertException(originalException);

        // Assert
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata);
        assertEquals(IllegalStateException.class.getName(), metadata.get("causeType"));
        assertEquals("Root cause", metadata.get("causeMessage"));
    }
}
