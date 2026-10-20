package com.nttdata.mscustomer.service;

import com.nttdata.mscustomer.exception.CustomerNotFoundException;
import com.nttdata.mscustomer.exception.DuplicateCustomerException;
import com.nttdata.mscustomer.document.CustomerDocument;
import com.nttdata.mscustomer.mapper.CustomerMapper;
import com.nttdata.mscustomer.model.CustomerChanges;
import com.nttdata.mscustomer.model.CustomerProfile;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.repository.CustomerRepository;
import java.time.Instant;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import com.nttdata.mscustomer.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerService(
            CustomerRepository customerRepository,
            CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    public Mono<Customer> createCustomer(Customer customer) {
        return customerRepository.existsByDocumentTypeAndDocumentNumber(
                customer.getDocumentType(),
                customer.getDocumentNumber()
        ).flatMap(exists -> {
            if (exists) {
                return Mono.error(new DuplicateCustomerException(
                        "Customer with document type "
                                + customer.getDocumentType()
                                + " and document number "
                                + customer.getDocumentNumber()
                                + " already exists"
                ));
            }

            return customerRepository.save(customerMapper.toDocument(customer))
                    .map(customerMapper::toDomain);
        });
    }

    public Flux<Customer> findCustomers(
            CustomerStatus status,
            CustomerType customerType,
            CustomerProfile profile) {
        CustomerDocument probe = new CustomerDocument();
        probe.setStatus(status);
        probe.setCustomerType(customerType);
        probe.setProfile(profile);

        return customerRepository.findAll(Example.of(probe))
                .map(customerMapper::toDomain);
    }

    public Mono<Customer> getCustomer(String id) {
        return findExisting(id).map(customerMapper::toDomain);
    }

    public Mono<Customer> updateCustomer(String id, CustomerChanges changes) {
        return findExisting(id)
                .flatMap(document -> {
                    Customer customer = customerMapper.toDomain(document);
                    customer.applyChanges(changes, Instant.now());
                    return customerRepository.save(customerMapper.toDocument(customer));
                })
                .map(customerMapper::toDomain);
    }

    public Mono<Void> deactivateCustomer(String id) {
        return findExisting(id)
                .flatMap(document -> {
                    Customer customer = customerMapper.toDomain(document);
                    if (!customer.deactivate(Instant.now())) {
                        return Mono.empty();
                    }
                    return customerRepository.save(customerMapper.toDocument(customer)).then();
                });
    }

    private Mono<CustomerDocument> findExisting(String id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new CustomerNotFoundException(
                        "Customer with id " + id + " not found")));
    }
}
