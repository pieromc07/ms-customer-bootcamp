package com.nttdata.mscustomer.service;

import static com.nttdata.mscustomer.util.ReactiveBridge.toFlowable;
import static com.nttdata.mscustomer.util.ReactiveBridge.toMaybe;
import static com.nttdata.mscustomer.util.ReactiveBridge.toSingle;

import com.nttdata.mscustomer.exception.CustomerNotFoundException;
import com.nttdata.mscustomer.exception.DuplicateCustomerException;
import com.nttdata.mscustomer.document.CustomerDocument;
import com.nttdata.mscustomer.event.CustomerEvent;
import com.nttdata.mscustomer.event.CustomerEventPublisher;
import com.nttdata.mscustomer.mapper.CustomerMapper;
import com.nttdata.mscustomer.model.Customer;
import com.nttdata.mscustomer.model.CustomerChanges;
import com.nttdata.mscustomer.model.CustomerProfile;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.repository.CustomerRepository;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_BYTES = 72;

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;
    private final PasswordEncoder passwordEncoder;
    private final CustomerEventPublisher eventPublisher;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper,
            PasswordEncoder passwordEncoder,
            CustomerEventPublisher eventPublisher) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    public Single<Customer> createCustomer(Customer customer, String rawPassword) {
        if (rawPassword == null) {
            return Single.error(() -> new IllegalArgumentException("Password is required"));
        }
        int passwordBytes = rawPassword.getBytes(StandardCharsets.UTF_8).length;
        if (rawPassword.length() < MIN_PASSWORD_LENGTH || passwordBytes > MAX_PASSWORD_BYTES) {
            return Single.error(() -> new IllegalArgumentException(
                    "Password must have between 8 and 72 bytes"));
        }

        return toSingle(customerRepository.existsByDocumentTypeAndDocumentNumber(
                customer.getDocumentType(),
                customer.getDocumentNumber()
        )).flatMap(exists -> {
            if (exists) {
                return Single.error(() -> new DuplicateCustomerException(
                        "Customer with document type "
                                + customer.getDocumentType()
                                + " and document number "
                                + customer.getDocumentNumber()
                                + " already exists"
                ));
            }

            customer.assignPasswordHash(passwordEncoder.encode(rawPassword));
            return toSingle(customerRepository.save(customerMapper.toDocument(customer)))
                    .map(customerMapper::toDomain)
                    .flatMap(created -> publishAfter(CustomerEvent.Type.CREATED, created));
        });
    }

    public Flowable<Customer> findCustomers(
            CustomerStatus status,
            CustomerType customerType,
            CustomerProfile profile) {
        CustomerDocument probe = new CustomerDocument();
        probe.setStatus(status);
        probe.setCustomerType(customerType);
        probe.setProfile(profile);

        return toFlowable(customerRepository.findAll(Example.of(probe)))
                .map(customerMapper::toDomain);
    }

    public Single<Customer> getCustomer(String id) {
        return findExisting(id).map(customerMapper::toDomain);
    }

    public Single<Customer> updateCustomer(String id, CustomerChanges changes) {
        return findExisting(id)
                .flatMap(document -> {
                    Customer customer = customerMapper.toDomain(document);
                    customer.applyChanges(changes, Instant.now());
                    return toSingle(customerRepository.save(customerMapper.toDocument(customer)));
                })
                .map(customerMapper::toDomain)
                .flatMap(updated -> publishAfter(CustomerEvent.Type.UPDATED, updated));
    }

    public Completable deactivateCustomer(String id) {
        return findExisting(id)
                .flatMapCompletable(document -> {
                    Customer customer = customerMapper.toDomain(document);
                    if (!customer.deactivate(Instant.now())) {
                        return Completable.complete();
                    }
                    return Completable.fromPublisher(
                                    customerRepository.save(customerMapper.toDocument(customer)))
                            .andThen(eventPublisher.publish(
                                    CustomerEvent.of(CustomerEvent.Type.DEACTIVATED, customer)));
                });
    }

    private Single<CustomerDocument> findExisting(String id) {
        return toMaybe(customerRepository.findById(id))
                .switchIfEmpty(Single.error(() -> new CustomerNotFoundException(
                        "Customer with id " + id + " not found")));
    }

    private Single<Customer> publishAfter(CustomerEvent.Type type, Customer customer) {
        return eventPublisher.publish(CustomerEvent.of(type, customer))
                .andThen(Single.just(customer));
    }
}
