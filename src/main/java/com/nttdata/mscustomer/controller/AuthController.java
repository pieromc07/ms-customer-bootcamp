package com.nttdata.mscustomer.controller;

import static com.nttdata.mscustomer.util.ReactiveBridge.toMono;

import com.nttdata.mscustomer.generated.api.AuthApi;
import com.nttdata.mscustomer.generated.dto.LoginRequest;
import com.nttdata.mscustomer.generated.dto.TokenResponse;
import com.nttdata.mscustomer.model.AccessToken;
import com.nttdata.mscustomer.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class AuthController implements AuthApi {

    private static final String BEARER = "Bearer";

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> login(
            Mono<LoginRequest> loginRequest,
            ServerWebExchange exchange) {

        return loginRequest
                .flatMap(request -> toMono(authService.login(
                        com.nttdata.mscustomer.model.DocumentType.valueOf(
                                request.getDocumentType().getValue()),
                        request.getDocumentNumber(),
                        request.getPassword())))
                .map(this::toResponse)
                .map(ResponseEntity::ok);
    }

    private TokenResponse toResponse(AccessToken token) {
        return new TokenResponse(token.value(), BEARER, token.expiresInSeconds());
    }
}
