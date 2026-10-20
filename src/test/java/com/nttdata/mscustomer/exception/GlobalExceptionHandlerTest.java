package com.nttdata.mscustomer.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.nttdata.mscustomer.generated.dto.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String PATH = "/api/v1/customers/665f1c2e8a1b2c3d4e5f6a7b";

    @Mock
    private ServerWebExchange exchange;
    @Mock
    private ServerHttpRequest request;
    @Mock
    private RequestPath requestPath;

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(exchange.getRequest()).thenReturn(request);
        when(request.getPath()).thenReturn(requestPath);
        when(requestPath.value()).thenReturn(PATH);
    }

    @Test
    void duplicateCustomerMapsToConflict() {
        ResponseEntity<ApiError> response = handler.handleDuplicateCustomer(
                new DuplicateCustomerException("duplicado"), exchange);

        assertError(response, HttpStatus.CONFLICT, "duplicado");
    }

    @Test
    void customerNotFoundMapsToNotFound() {
        ResponseEntity<ApiError> response = handler.handleCustomerNotFound(
                new CustomerNotFoundException("no existe"), exchange);

        assertError(response, HttpStatus.NOT_FOUND, "no existe");
    }

    @Test
    void inactiveCustomerMapsToConflict() {
        ResponseEntity<ApiError> response = handler.handleInactiveCustomer(
                new InactiveCustomerException("inactivo"), exchange);

        assertError(response, HttpStatus.CONFLICT, "inactivo");
    }

    @Test
    void illegalArgumentMapsToBadRequest() {
        ResponseEntity<ApiError> response = handler.handleInvalidCustomer(
                new IllegalArgumentException("dni invalido"), exchange);

        assertError(response, HttpStatus.BAD_REQUEST, "dni invalido");
    }

    @Test
    void serverWebInputExceptionMapsToBadRequestWithGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleInvalidRequest(
                new ServerWebInputException("json mal formado", null), exchange);

        assertError(response, HttpStatus.BAD_REQUEST, "Invalid customer data");
    }

    private static void assertError(
            ResponseEntity<ApiError> response, HttpStatus status, String message) {
        assertEquals(status, response.getStatusCode());
        ApiError body = response.getBody();
        assertEquals(status.value(), body.getStatus());
        assertEquals(status.name(), body.getError());
        assertEquals(message, body.getMessage());
        assertEquals(PATH, body.getPath());
    }
}
