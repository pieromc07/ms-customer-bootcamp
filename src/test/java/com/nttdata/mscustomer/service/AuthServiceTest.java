package com.nttdata.mscustomer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.mscustomer.document.CustomerDocument;
import com.nttdata.mscustomer.exception.InvalidCredentialsException;
import com.nttdata.mscustomer.mapper.CustomerMapper;
import com.nttdata.mscustomer.model.AccessToken;
import com.nttdata.mscustomer.model.Customer;
import com.nttdata.mscustomer.model.CustomerProfile;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.model.DocumentType;
import com.nttdata.mscustomer.model.PersonalDetails;
import com.nttdata.mscustomer.repository.CustomerRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String PASSWORD = "ClaveSegura1";
    private static final Instant CREATED = Instant.parse("2024-01-01T00:00:00Z");

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private JwtTokenIssuer tokenIssuer;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;
    private String storedHash;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                customerRepository, new CustomerMapper(), passwordEncoder, tokenIssuer);
        storedHash = passwordEncoder.encode(PASSWORD);
    }

    @Test
    void loginReturnsTokenForActiveCustomerWithCorrectPassword() {
        stubFind(document(CustomerStatus.ACTIVE, storedHash));
        AccessToken token = new AccessToken("jwt-value", 3600L);
        when(tokenIssuer.issue(any(Customer.class))).thenReturn(token);

        AccessToken result = authService
                .login(DocumentType.DNI, "12345678", PASSWORD)
                .blockingGet();

        assertEquals(token, result);
    }

    @Test
    void loginRejectsUnknownDocumentWithInvalidCredentials() {
        when(customerRepository.findByDocumentTypeAndDocumentNumber(
                DocumentType.DNI, "12345678")).thenReturn(Mono.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(DocumentType.DNI, "12345678", PASSWORD).blockingGet());

        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void loginRejectsInactiveCustomerWithInvalidCredentials() {
        stubFind(document(CustomerStatus.INACTIVE, storedHash));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(DocumentType.DNI, "12345678", PASSWORD).blockingGet());

        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void loginRejectsWrongPasswordWithInvalidCredentials() {
        stubFind(document(CustomerStatus.ACTIVE, storedHash));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(DocumentType.DNI, "12345678", "OtraClave99")
                        .blockingGet());

        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void loginRejectsCustomerWithoutPasswordHash() {
        stubFind(document(CustomerStatus.ACTIVE, null));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(DocumentType.DNI, "12345678", PASSWORD).blockingGet());

        verify(tokenIssuer, never()).issue(any());
    }

    @Test
    void loginRejectsMissingPasswordWithInvalidCredentials() {
        stubFind(document(CustomerStatus.ACTIVE, storedHash));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(DocumentType.DNI, "12345678", null).blockingGet());

        verify(tokenIssuer, never()).issue(any());
    }

    private void stubFind(CustomerDocument document) {
        when(customerRepository.findByDocumentTypeAndDocumentNumber(
                DocumentType.DNI, "12345678")).thenReturn(Mono.just(document));
    }

    private static CustomerDocument document(CustomerStatus status, String hash) {
        CustomerDocument document = new CustomerDocument();
        document.setId("665f1c2e8a1b2c3d4e5f6a7b");
        document.setCustomerType(CustomerType.PERSONAL);
        document.setProfile(CustomerProfile.STANDARD);
        document.setDocumentType(DocumentType.DNI);
        document.setDocumentNumber("12345678");
        document.setEmail("ana@example.com");
        document.setPhone("999111222");
        document.setStatus(status);
        document.setPersonalDetails(new PersonalDetails("Ana", "Lopez"));
        document.setPasswordHash(hash);
        document.setCreatedAt(CREATED);
        document.setUpdatedAt(CREATED);
        return document;
    }
}
