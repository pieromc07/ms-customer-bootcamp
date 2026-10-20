package com.nttdata.mscustomer.service;

import static com.nttdata.mscustomer.util.ReactiveBridge.toMaybe;

import com.nttdata.mscustomer.exception.InvalidCredentialsException;
import com.nttdata.mscustomer.mapper.CustomerMapper;
import com.nttdata.mscustomer.model.AccessToken;
import com.nttdata.mscustomer.model.Customer;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.DocumentType;
import com.nttdata.mscustomer.repository.CustomerRepository;
import io.reactivex.rxjava3.core.Single;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenIssuer tokenIssuer;

    public AuthService(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenIssuer tokenIssuer) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
    }

    public Single<AccessToken> login(
            DocumentType documentType,
            String documentNumber,
            String rawPassword) {
        return toMaybe(customerRepository.findByDocumentTypeAndDocumentNumber(
                        documentType,
                        documentNumber))
                .map(customerMapper::toDomain)
                .filter(customer -> canAuthenticate(customer, rawPassword))
                .map(tokenIssuer::issue)
                .switchIfEmpty(Single.error(() -> new InvalidCredentialsException()));
    }

    private boolean canAuthenticate(Customer customer, String rawPassword) {
        return customer.getStatus() == CustomerStatus.ACTIVE
                && customer.getPasswordHash() != null
                && rawPassword != null
                && passwordEncoder.matches(rawPassword, customer.getPasswordHash());
    }
}
