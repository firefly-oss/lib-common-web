package com.firefly.common.web.error.examples;

import com.firefly.common.web.error.exceptions.BusinessException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeoutException;

/**
 * Example controller that demonstrates how to use the exception handling features.
 * This controller shows how to throw business exceptions directly and how to use
 * the exception conversion mechanism in a REST controller.
 */
@RestController
@RequestMapping("/api/examples/exceptions")
public class ExceptionHandlingController {

    private final ExceptionHandlingExample exampleService;

    /**
     * Creates a new ExceptionHandlingController with the given example service.
     *
     * @param exampleService the example service
     */
    public ExceptionHandlingController(ExceptionHandlingExample exampleService) {
        this.exampleService = exampleService;
    }

    /**
     * Example endpoint that throws business exceptions directly.
     *
     * @param type the type of exception to throw
     * @return a Mono that completes with an error
     */
    @GetMapping("/business/{type}")
    public Mono<String> throwBusinessException(@PathVariable String type) {
        return Mono.defer(() -> {
            exampleService.throwBusinessException(type);
            return Mono.just("This should never be returned");
        });
    }

    /**
     * Example endpoint that manually converts standard exceptions to business exceptions.
     *
     * @param type the type of exception to convert
     * @return a Mono that completes with an error
     */
    @GetMapping("/convert/{type}")
    public Mono<String> convertStandardException(@PathVariable String type) {
        return Mono.defer(() -> {
            BusinessException exception = exampleService.convertStandardException(type);
            return Mono.error(exception);
        });
    }

    /**
     * Example endpoint that automatically converts standard exceptions to business exceptions.
     * The GlobalExceptionHandler will automatically convert any exceptions to business exceptions.
     *
     * @param type the type of exception to throw
     * @return a Mono that completes with an error
     */
    @GetMapping("/auto-convert/{type}")
    public Mono<String> throwStandardException(@PathVariable String type) {
        return Mono.defer(() -> {
            try {
                exampleService.throwStandardException(type);
                return Mono.just("This should never be returned");
            } catch (TimeoutException e) {
                // The GlobalExceptionHandler will automatically convert this to a BusinessException
                throw new RuntimeException("Timeout occurred", e);
            }
        });
    }
}
