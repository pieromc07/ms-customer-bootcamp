package com.nttdata.mscustomer.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.mscustomer.generated.dto.CustomerProfile;
import com.nttdata.mscustomer.generated.dto.CustomerStatus;
import com.nttdata.mscustomer.generated.dto.CustomerType;
import com.nttdata.mscustomer.generated.dto.PatchCustomerRequest;
import com.nttdata.mscustomer.generated.dto.UpdateCustomerRequest;
import com.nttdata.mscustomer.mapper.CustomerMapper;
import com.nttdata.mscustomer.model.Customer;
import com.nttdata.mscustomer.model.CustomerChanges;
import com.nttdata.mscustomer.model.DocumentType;
import com.nttdata.mscustomer.model.PersonalDetails;
import com.nttdata.mscustomer.service.CustomerService;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private static final String ID = "665f1c2e8a1b2c3d4e5f6a7b";
    private static final Instant NOW = Instant.parse("2024-01-01T00:00:00Z");

    @Mock
    private CustomerService customerService;

    private CustomerController controller;

    @BeforeEach
    void setUp() {
        controller = new CustomerController(customerService, new CustomerMapper());
    }

    @Test
    void getCustomerByIdReturnsOk() {
        when(customerService.getCustomer(ID)).thenReturn(Mono.just(domainCustomer()));

        ResponseEntity<com.nttdata.mscustomer.generated.dto.Customer> response =
                controller.getCustomerById(ID, null).block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ID, response.getBody().getId());
        assertEquals(CustomerStatus.ACTIVE, response.getBody().getStatus());
    }

    @Test
    void getAllCustomersMapsGeneratedFiltersToDomain() {
        when(customerService.findCustomers(
                com.nttdata.mscustomer.model.CustomerStatus.ACTIVE,
                com.nttdata.mscustomer.model.CustomerType.PERSONAL,
                com.nttdata.mscustomer.model.CustomerProfile.VIP))
                .thenReturn(Flux.just(domainCustomer()));

        ResponseEntity<Flux<com.nttdata.mscustomer.generated.dto.Customer>> response =
                controller.getAllCustomers(
                        CustomerStatus.ACTIVE, CustomerType.PERSONAL,
                        CustomerProfile.VIP, null).block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().collectList().block().size());
    }

    @Test
    void deactivateCustomerReturnsNoContent() {
        when(customerService.deactivateCustomer(ID)).thenReturn(Mono.empty());

        ResponseEntity<Void> response = controller.deactivateCustomer(ID, null).block();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void patchWithNullEmailPassesClearToService() {
        when(customerService.updateCustomer(eq(ID), any(CustomerChanges.class)))
                .thenReturn(Mono.just(domainCustomer()));
        PatchCustomerRequest request = new PatchCustomerRequest();
        request.setEmail(org.openapitools.jackson.nullable.JsonNullable.of(null));

        ResponseEntity<com.nttdata.mscustomer.generated.dto.Customer> response =
                controller.patchCustomer(ID, Mono.just(request), null).block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<CustomerChanges> captor = ArgumentCaptor.forClass(CustomerChanges.class);
        verify(customerService).updateCustomer(eq(ID), captor.capture());
        assertTrue(captor.getValue().email().provided());
        assertNull(captor.getValue().email().value());
        assertFalse(captor.getValue().phone().provided());
    }

    @Test
    void putWithOmittedEmailClearsContactAndReturnsOk() {
        when(customerService.updateCustomer(eq(ID), any(CustomerChanges.class)))
                .thenReturn(Mono.just(domainCustomer()));
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setPersonalDetails(new com.nttdata.mscustomer.generated.dto.PersonalDetails("Ana", "Lopez"));

        ResponseEntity<com.nttdata.mscustomer.generated.dto.Customer> response =
                controller.updateCustomer(ID, Mono.just(request), null).block();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ID, response.getBody().getId());
        ArgumentCaptor<CustomerChanges> captor = ArgumentCaptor.forClass(CustomerChanges.class);
        verify(customerService).updateCustomer(eq(ID), captor.capture());
        assertTrue(captor.getValue().email().provided());
        assertNull(captor.getValue().email().value());
        assertEquals("Lopez", captor.getValue().personalDetails().lastName());
    }

    private static Customer domainCustomer() {
        return new Customer(
                ID,
                com.nttdata.mscustomer.model.CustomerType.PERSONAL,
                com.nttdata.mscustomer.model.CustomerProfile.VIP,
                DocumentType.DNI,
                "12345678",
                "ana@example.com",
                "999111222",
                com.nttdata.mscustomer.model.CustomerStatus.ACTIVE,
                new PersonalDetails("Ana", "Lopez"),
                null,
                NOW,
                NOW);
    }
}
