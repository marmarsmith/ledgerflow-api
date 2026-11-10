
package com.marcel.ledgerflow.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturn409ForOptimisticLockConflict() {
        // Arrange
        ObjectOptimisticLockingFailureException exception =
                new ObjectOptimisticLockingFailureException(
                        "User",
                        1L
                );

        // Act
        ResponseEntity<Map<String, String>> response =
                handler.handleOptimisticLockConflict(exception);

        // Assert
        assertEquals(
                HttpStatus.CONFLICT,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertEquals(
                "This information was updated by another request. Please refresh and try again.",
                response.getBody().get("error")
        );
    }
}
