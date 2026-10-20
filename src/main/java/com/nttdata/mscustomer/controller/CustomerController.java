package com.nttdata.mscustomer.controller;

import static com.nttdata.mscustomer.util.ReactiveBridge.toFlux;
import static com.nttdata.mscustomer.util.ReactiveBridge.toMono;
import static com.nttdata.mscustomer.util.ReactiveBridge.toVoid;

import com.nttdata.mscustomer.generated.api.CustomersApi;
import com.nttdata.mscustomer.generated.dto.CreateCustomerRequest;
import com.nttdata.mscustomer.generated.dto.Customer;
import com.nttdata.mscustomer.generated.dto.CustomerProfile;
import com.nttdata.mscustomer.generated.dto.CustomerStatus;
import com.nttdata.mscustomer.generated.dto.CustomerType;
import com.nttdata.mscustomer.generated.dto.PatchCustomerRequest;
import com.nttdata.mscustomer.generated.dto.UpdateCustomerRequest;
import com.nttdata.mscustomer.mapper.CustomerMapper;
import com.nttdata.mscustomer.service.CustomerService;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class CustomerController implements CustomersApi {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public CustomerController(
            CustomerService customerService,
            CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @Override
    public Mono<ResponseEntity<Customer>> createCustomer(
            Mono<CreateCustomerRequest> createCustomerRequest,
            ServerWebExchange exchange) {

        return createCustomerRequest
                .map(customerMapper::toDomain)
                .flatMap(customer -> toMono(customerService.createCustomer(customer)))
                .map(customerMapper::toResponse)
                .map(response -> ResponseEntity
                        .created(URI.create("/api/v1/customers/" + response.getId()))
                        .body(response));
    }

    @Override
    public Mono<ResponseEntity<Flux<Customer>>> getAllCustomers(
            CustomerStatus status,
            CustomerType customerType,
            CustomerProfile profile,
            ServerWebExchange exchange) {

        Flux<Customer> customers = toFlux(customerService.findCustomers(
                        customerMapper.toDomainStatus(status),
                        customerMapper.toDomainCustomerType(customerType),
                        customerMapper.toDomainProfile(profile)))
                .map(customerMapper::toResponse);
        return Mono.just(ResponseEntity.ok(customers));
    }

    @Override
    public Mono<ResponseEntity<Customer>> getCustomerById(
            String id,
            ServerWebExchange exchange) {

        return toMono(customerService.getCustomer(id))
                .map(customerMapper::toResponse)
                .map(response -> ResponseEntity.ok(response));
    }

    @Override
    public Mono<ResponseEntity<Customer>> updateCustomer(
            String id,
            Mono<UpdateCustomerRequest> updateCustomerRequest,
            ServerWebExchange exchange) {

        return updateCustomerRequest
                .map(request -> customerMapper.toChanges(request))
                .flatMap(changes -> toMono(customerService.updateCustomer(id, changes)))
                .map(customerMapper::toResponse)
                .map(response -> ResponseEntity.ok(response));
    }

    @Override
    public Mono<ResponseEntity<Customer>> patchCustomer(
            String id,
            Mono<PatchCustomerRequest> patchCustomerRequest,
            ServerWebExchange exchange) {

        return patchCustomerRequest
                .map(request -> customerMapper.toChanges(request))
                .flatMap(changes -> toMono(customerService.updateCustomer(id, changes)))
                .map(customerMapper::toResponse)
                .map(response -> ResponseEntity.ok(response));
    }

    @Override
    public Mono<ResponseEntity<Void>> deactivateCustomer(
            String id,
            ServerWebExchange exchange) {

        return toVoid(customerService.deactivateCustomer(id))
                .then(Mono.fromSupplier(() -> ResponseEntity.<Void>noContent().build()));
    }
}
