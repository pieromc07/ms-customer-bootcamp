package com.nttdata.mscustomer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nttdata.mscustomer.document.CustomerDocument;
import com.nttdata.mscustomer.event.CustomerEvent;
import com.nttdata.mscustomer.event.CustomerEventPublisher;
import com.nttdata.mscustomer.exception.CustomerNotFoundException;
import com.nttdata.mscustomer.exception.DuplicateCustomerException;
import com.nttdata.mscustomer.exception.InactiveCustomerException;
import com.nttdata.mscustomer.mapper.CustomerMapper;
import com.nttdata.mscustomer.model.ContactUpdate;
import com.nttdata.mscustomer.model.Customer;
import com.nttdata.mscustomer.model.CustomerChanges;
import com.nttdata.mscustomer.model.CustomerProfile;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.model.DocumentType;
import com.nttdata.mscustomer.model.PersonalDetails;
import com.nttdata.mscustomer.repository.CustomerRepository;
import io.reactivex.rxjava3.core.Completable;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Example;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String ID = "665f1c2e8a1b2c3d4e5f6a7b";
    private static final Instant CREATED = Instant.parse("2024-01-01T00:00:00Z");
    private static final String PASSWORD = "ClaveSegura1";
    private static final String HASH = "$2a$10$hashedvalue";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomerEventPublisher eventPublisher;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(
                customerRepository, new CustomerMapper(), passwordEncoder, eventPublisher);
        lenient().when(eventPublisher.publish(any(CustomerEvent.class)))
                .thenReturn(Completable.complete());
    }

    @Test
    void createCustomerPublishesCreatedEvent() {
        when(customerRepository.existsByDocumentTypeAndDocumentNumber(
                DocumentType.DNI, "12345678")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(customerRepository.save(any(CustomerDocument.class)))
                .thenReturn(Mono.just(document(CustomerStatus.ACTIVE)));

        customerService.createCustomer(newPersonal(), PASSWORD).blockingGet();

        ArgumentCaptor<CustomerEvent> captor = ArgumentCaptor.forClass(CustomerEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertEquals("customer.created", captor.getValue().eventType());
        assertEquals(ID, captor.getValue().customerId());
        assertEquals(CustomerStatus.ACTIVE, captor.getValue().status());
        assertEquals(1, captor.getValue().schemaVersion());
    }

    @Test
    void createCustomerDoesNotPublishWhenDuplicate() {
        when(customerRepository.existsByDocumentTypeAndDocumentNumber(
                DocumentType.DNI, "12345678")).thenReturn(Mono.just(true));

        assertThrows(DuplicateCustomerException.class,
                () -> customerService.createCustomer(newPersonal(), PASSWORD).blockingGet());

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void updateCustomerPublishesUpdatedEvent() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.ACTIVE)));
        when(customerRepository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        CustomerChanges changes = new CustomerChanges(
                ContactUpdate.set("new@example.com"), ContactUpdate.KEEP, null, null);

        customerService.updateCustomer(ID, changes).blockingGet();

        ArgumentCaptor<CustomerEvent> captor = ArgumentCaptor.forClass(CustomerEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertEquals("customer.updated", captor.getValue().eventType());
        assertEquals(ID, captor.getValue().customerId());
    }

    @Test
    void updateCustomerDoesNotPublishWhenRejected() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.INACTIVE)));
        CustomerChanges changes = new CustomerChanges(
                ContactUpdate.set("new@example.com"), ContactUpdate.KEEP, null, null);

        assertThrows(InactiveCustomerException.class,
                () -> customerService.updateCustomer(ID, changes).blockingGet());

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void deactivateCustomerPublishesDeactivatedEvent() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.ACTIVE)));
        when(customerRepository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        customerService.deactivateCustomer(ID).blockingAwait();

        ArgumentCaptor<CustomerEvent> captor = ArgumentCaptor.forClass(CustomerEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertEquals("customer.deactivated", captor.getValue().eventType());
        assertEquals(CustomerStatus.INACTIVE, captor.getValue().status());
    }

    @Test
    void deactivateAlreadyInactiveCustomerDoesNotPublish() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.INACTIVE)));

        customerService.deactivateCustomer(ID).blockingAwait();

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void createCustomerRejectsDuplicateDocument() {
        when(customerRepository.existsByDocumentTypeAndDocumentNumber(
                DocumentType.DNI, "12345678")).thenReturn(Mono.just(true));

        assertThrows(DuplicateCustomerException.class,
                () -> customerService.createCustomer(newPersonal(), PASSWORD).blockingGet());

        verify(customerRepository, never()).save(any());
    }

    @Test
    void createCustomerSavesNewCustomerAsActive() {
        when(customerRepository.existsByDocumentTypeAndDocumentNumber(
                DocumentType.DNI, "12345678")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(customerRepository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Customer saved = customerService.createCustomer(newPersonal(), PASSWORD).blockingGet();

        assertEquals(CustomerStatus.ACTIVE, saved.getStatus());
        assertEquals("12345678", saved.getDocumentNumber());
        verify(customerRepository, times(1)).save(any(CustomerDocument.class));
    }

    @Test
    void createCustomerStoresOnlyTheEncodedPassword() {
        when(customerRepository.existsByDocumentTypeAndDocumentNumber(
                DocumentType.DNI, "12345678")).thenReturn(Mono.just(false));
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(customerRepository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        Customer saved = customerService.createCustomer(newPersonal(), PASSWORD).blockingGet();

        ArgumentCaptor<CustomerDocument> captor = ArgumentCaptor.forClass(CustomerDocument.class);
        verify(customerRepository).save(captor.capture());
        assertEquals(HASH, captor.getValue().getPasswordHash());
        assertEquals(HASH, saved.getPasswordHash());
        assertNotEquals(PASSWORD, captor.getValue().getPasswordHash());
    }

    @Test
    void createCustomerRejectsMissingPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(newPersonal(), null).blockingGet());

        verifyNoInteractions(customerRepository, passwordEncoder);
    }

    @Test
    void createCustomerRejectsPasswordShorterThanEightCharacters() {
        assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(newPersonal(), "short").blockingGet());

        verifyNoInteractions(customerRepository, passwordEncoder);
    }

    @Test
    void createCustomerRejectsPasswordLongerThanSeventyTwoBytes() {
        String longPassword = "ñ".repeat(37);

        assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(newPersonal(), longPassword).blockingGet());

        verifyNoInteractions(customerRepository, passwordEncoder);
    }

    @Test
    void getCustomerThrowsWhenMissing() {
        when(customerRepository.findById(ID)).thenReturn(Mono.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.getCustomer(ID).blockingGet());
    }

    @Test
    void getCustomerReturnsInactiveCustomer() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.INACTIVE)));

        Customer customer = customerService.getCustomer(ID).blockingGet();

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
    }

    @Test
    void updateCustomerRejectsInactiveCustomer() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.INACTIVE)));
        CustomerChanges changes = new CustomerChanges(
                ContactUpdate.set("new@example.com"), ContactUpdate.KEEP, null, null);

        assertThrows(InactiveCustomerException.class,
                () -> customerService.updateCustomer(ID, changes).blockingGet());

        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateCustomerAppliesChangesAndKeepsCreatedAt() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.ACTIVE)));
        when(customerRepository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        CustomerChanges changes = new CustomerChanges(
                ContactUpdate.set(null), ContactUpdate.KEEP,
                new PersonalDetails("Ana", "Perez"), null);

        Customer updated = customerService.updateCustomer(ID, changes).blockingGet();

        assertNull(updated.getEmail());
        assertEquals("999111222", updated.getPhone());
        assertEquals("Perez", updated.getPersonalDetails().lastName());
        assertEquals(CustomerType.PERSONAL, updated.getCustomerType());
        assertEquals(CREATED, updated.getCreatedAt());
        assertEquals(ID, updated.getId());
    }

    @Test
    void updateCustomerThrowsWhenMissing() {
        when(customerRepository.findById(ID)).thenReturn(Mono.empty());
        CustomerChanges changes = new CustomerChanges(ContactUpdate.KEEP, ContactUpdate.KEEP, null, null);

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.updateCustomer(ID, changes).blockingGet());

        verify(customerRepository, never()).save(any());
    }

    @Test
    void deactivateCustomerSavesInactiveStatus() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.ACTIVE)));
        when(customerRepository.save(any(CustomerDocument.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        customerService.deactivateCustomer(ID).blockingAwait();

        ArgumentCaptor<CustomerDocument> captor = ArgumentCaptor.forClass(CustomerDocument.class);
        verify(customerRepository).save(captor.capture());
        assertEquals(CustomerStatus.INACTIVE, captor.getValue().getStatus());
    }

    @Test
    void deactivateAlreadyInactiveCustomerDoesNotSave() {
        when(customerRepository.findById(ID)).thenReturn(Mono.just(document(CustomerStatus.INACTIVE)));

        customerService.deactivateCustomer(ID).blockingAwait();

        verify(customerRepository, never()).save(any());
    }

    @Test
    void deactivateMissingCustomerThrowsNotFound() {
        when(customerRepository.findById(ID)).thenReturn(Mono.empty());

        assertThrows(CustomerNotFoundException.class,
                () -> customerService.deactivateCustomer(ID).blockingAwait());
    }

    @Test
    void findCustomersBuildsProbeFromFilters() {
        when(customerRepository.findAll(org.mockito.ArgumentMatchers.<Example<CustomerDocument>>any()))
                .thenReturn(Flux.just(document(CustomerStatus.ACTIVE)));

        customerService.findCustomers(CustomerStatus.ACTIVE, null, CustomerProfile.VIP)
                .toList().blockingGet();

        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<Example<CustomerDocument>> captor =
                ArgumentCaptor.forClass((Class) Example.class);
        verify(customerRepository).findAll(captor.capture());
        CustomerDocument probe = captor.getValue().getProbe();
        assertEquals(CustomerStatus.ACTIVE, probe.getStatus());
        assertNull(probe.getCustomerType());
        assertEquals(CustomerProfile.VIP, probe.getProfile());
    }

    private static Customer newPersonal() {
        return Customer.create(
                CustomerType.PERSONAL, CustomerProfile.STANDARD, DocumentType.DNI,
                "12345678", "ana@example.com", "999111222",
                new PersonalDetails("Ana", "Lopez"), null);
    }

    private static CustomerDocument document(CustomerStatus status) {
        CustomerDocument document = new CustomerDocument();
        document.setId(ID);
        document.setCustomerType(CustomerType.PERSONAL);
        document.setProfile(CustomerProfile.STANDARD);
        document.setDocumentType(DocumentType.DNI);
        document.setDocumentNumber("12345678");
        document.setEmail("ana@example.com");
        document.setPhone("999111222");
        document.setStatus(status);
        document.setPersonalDetails(new PersonalDetails("Ana", "Lopez"));
        document.setBusinessDetails(null);
        document.setCreatedAt(CREATED);
        document.setUpdatedAt(CREATED);
        return document;
    }
}
