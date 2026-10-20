package com.nttdata.mscustomer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nttdata.mscustomer.exception.InactiveCustomerException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CustomerTest {

    private static final PersonalDetails PERSONAL = new PersonalDetails("Ana", "Lopez");
    private static final BusinessDetails BUSINESS = new BusinessDetails("ACME SAC", null);

    @Test
    void createReturnsActiveCustomerWithoutId() {
        Customer customer = personalCustomer("12345678");

        assertNull(customer.getId());
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        assertNotNull(customer.getCreatedAt());
        assertEquals(customer.getCreatedAt(), customer.getUpdatedAt());
    }

    @Test
    void createRejectsDniWithSevenDigits() {
        assertThrows(IllegalArgumentException.class, () -> personalCustomer("1234567"));
    }

    @Test
    void createRejectsDniWithLetters() {
        assertThrows(IllegalArgumentException.class, () -> personalCustomer("1234567A"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"10", "15", "16", "17", "20"})
    void createAcceptsRucWithAllowedPrefixes(String prefix) {
        Customer customer = businessCustomer(prefix + "123456789");

        assertEquals(CustomerType.BUSINESS, customer.getCustomerType());
    }

    @Test
    void createRejectsRucWithForbiddenPrefix() {
        assertThrows(IllegalArgumentException.class, () -> businessCustomer("30123456789"));
    }

    @Test
    void createRejectsRucWithWrongLength() {
        assertThrows(IllegalArgumentException.class, () -> businessCustomer("2012345678"));
    }

    @Test
    void createRejectsPersonalCustomerWithPymeProfile() {
        assertThrows(IllegalArgumentException.class, () -> Customer.create(
                CustomerType.PERSONAL, CustomerProfile.PYME, DocumentType.DNI,
                "12345678", null, null, PERSONAL, null));
    }

    @Test
    void createRejectsPersonalCustomerWithRuc() {
        assertThrows(IllegalArgumentException.class, () -> Customer.create(
                CustomerType.PERSONAL, CustomerProfile.STANDARD, DocumentType.RUC,
                "20123456789", null, null, PERSONAL, null));
    }

    @Test
    void createRejectsBusinessCustomerWithPersonalDetails() {
        assertThrows(IllegalArgumentException.class, () -> Customer.create(
                CustomerType.BUSINESS, CustomerProfile.STANDARD, DocumentType.RUC,
                "20123456789", null, null, PERSONAL, BUSINESS));
    }

    @Test
    void createRejectsPersonalCustomerWithBusinessDetails() {
        assertThrows(IllegalArgumentException.class, () -> Customer.create(
                CustomerType.PERSONAL, CustomerProfile.STANDARD, DocumentType.DNI,
                "12345678", null, null, PERSONAL, BUSINESS));
    }

    @Test
    void applyChangesKeepsContactWhenNotProvided() {
        Customer customer = personalCustomer("12345678");

        customer.applyChanges(new CustomerChanges(
                ContactUpdate.KEEP, ContactUpdate.KEEP, null, null), Instant.now());

        assertEquals("ana@example.com", customer.getEmail());
        assertEquals("999111222", customer.getPhone());
    }

    @Test
    void applyChangesClearsContactWhenSetToNull() {
        Customer customer = personalCustomer("12345678");

        customer.applyChanges(new CustomerChanges(
                ContactUpdate.set(null), ContactUpdate.KEEP, null, null), Instant.now());

        assertNull(customer.getEmail());
        assertEquals("999111222", customer.getPhone());
    }

    @Test
    void applyChangesRefreshesUpdatedAtAndKeepsCreatedAt() {
        Customer customer = personalCustomer("12345678");
        Instant createdAt = customer.getCreatedAt();
        Instant later = createdAt.plusSeconds(60);

        customer.applyChanges(new CustomerChanges(
                ContactUpdate.KEEP, ContactUpdate.KEEP,
                new PersonalDetails("Ana", "Perez"), null), later);

        assertEquals(later, customer.getUpdatedAt());
        assertEquals(createdAt, customer.getCreatedAt());
        assertEquals("Perez", customer.getPersonalDetails().lastName());
    }

    @Test
    void applyChangesRejectsDetailsOfWrongKind() {
        Customer customer = personalCustomer("12345678");

        assertThrows(IllegalArgumentException.class, () -> customer.applyChanges(
                new CustomerChanges(ContactUpdate.KEEP, ContactUpdate.KEEP,
                        null, new BusinessDetails("ACME SAC", null)),
                Instant.now()));
    }

    @Test
    void applyChangesOnInactiveCustomerThrows() {
        Customer customer = personalCustomer("12345678");
        customer.deactivate(Instant.now());

        assertThrows(InactiveCustomerException.class, () -> customer.applyChanges(
                new CustomerChanges(ContactUpdate.set("x@example.com"),
                        ContactUpdate.KEEP, null, null),
                Instant.now()));
    }

    @Test
    void deactivateReturnsTrueOnceThenFalse() {
        Customer customer = personalCustomer("12345678");

        assertTrue(customer.deactivate(Instant.now()));
        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        assertFalse(customer.deactivate(Instant.now()));
    }

    private static Customer personalCustomer(String dni) {
        return Customer.create(
                CustomerType.PERSONAL, CustomerProfile.STANDARD, DocumentType.DNI,
                dni, "ana@example.com", "999111222", PERSONAL, null);
    }

    private static Customer businessCustomer(String ruc) {
        return Customer.create(
                CustomerType.BUSINESS, CustomerProfile.PYME, DocumentType.RUC,
                ruc, null, null, null, BUSINESS);
    }
}
