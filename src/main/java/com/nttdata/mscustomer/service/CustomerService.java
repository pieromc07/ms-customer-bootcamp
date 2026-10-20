package com.nttdata.mscustomer.service;

import static com.nttdata.mscustomer.util.ReactiveBridge.toFlowable;
import static com.nttdata.mscustomer.util.ReactiveBridge.toMaybe;
import static com.nttdata.mscustomer.util.ReactiveBridge.toSingle;

import com.nttdata.mscustomer.exception.CustomerNotFoundException;
import com.nttdata.mscustomer.exception.DuplicateCustomerException;
import com.nttdata.mscustomer.document.CustomerDocument;
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
import java.time.Instant;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;

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

    public Single<Customer> createCustomer(Customer customer) {
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

            return toSingle(customerRepository.save(customerMapper.toDocument(customer)))
                    .map(customerMapper::toDomain);
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
                .map(customerMapper::toDomain);
    }

    public Completable deactivateCustomer(String id) {
        return findExisting(id)
                .flatMapCompletable(document -> {
                    Customer customer = customerMapper.toDomain(document);
                    if (!customer.deactivate(Instant.now())) {
                        return Completable.complete();
                    }
                    return Completable.fromPublisher(
                            customerRepository.save(customerMapper.toDocument(customer)));
                });
    }

    private Single<CustomerDocument> findExisting(String id) {
        return toMaybe(customerRepository.findById(id))
                .switchIfEmpty(Single.error(() -> new CustomerNotFoundException(
                        "Customer with id " + id + " not found")));
    }
}
