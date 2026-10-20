package com.nttdata.mscustomer.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nttdata.mscustomer.generated.dto.BusinessDetails;
import com.nttdata.mscustomer.generated.dto.CreateCustomerRequest;
import com.nttdata.mscustomer.generated.dto.PatchCustomerRequest;
import com.nttdata.mscustomer.generated.dto.PersonalDetails;
import com.nttdata.mscustomer.generated.dto.UpdateCustomerRequest;
import com.nttdata.mscustomer.model.Customer;
import com.nttdata.mscustomer.model.CustomerChanges;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.config.JacksonConfig;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import tools.jackson.databind.json.JsonMapper;

class CustomerMapperTest {

    private final CustomerMapper mapper = new CustomerMapper();

    @Test
    void toDomainCreatesActiveCustomerFromRequest() {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CreateCustomerRequest.CustomerTypeEnum.PERSONAL,
                CreateCustomerRequest.DocumentTypeEnum.DNI,
                "12345678",
                "ClaveSegura1",
                CreateCustomerRequest.ProfileEnum.VIP);
        request.setEmail(JsonNullable.of("ana@example.com"));
        request.setPhone(JsonNullable.undefined());
        request.setPersonalDetails(new PersonalDetails("Ana", "Lopez"));

        Customer customer = mapper.toDomain(request);

        assertEquals(CustomerType.PERSONAL, customer.getCustomerType());
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        assertEquals("ana@example.com", customer.getEmail());
        assertNull(customer.getPhone());
    }

    @Test
    void toChangesForPutClearsOmittedContact() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setPersonalDetails(new PersonalDetails("Ana", "Perez"));

        CustomerChanges changes = mapper.toChanges(request);

        assertTrue(changes.email().provided());
        assertNull(changes.email().value());
        assertTrue(changes.phone().provided());
        assertNull(changes.phone().value());
        assertEquals("Perez", changes.personalDetails().lastName());
        assertNull(changes.businessDetails());
    }

    @Test
    void toChangesForPutRejectsMissingDetails() {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toChanges(new UpdateCustomerRequest()));
    }

    @Test
    void toChangesForPutRejectsBothDetails() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setPersonalDetails(new PersonalDetails("Ana", "Perez"));
        request.setBusinessDetails(new BusinessDetails("ACME SAC"));

        assertThrows(IllegalArgumentException.class, () -> mapper.toChanges(request));
    }

    @Test
    void toChangesForPatchKeepsUndefinedFields() {
        PatchCustomerRequest request = new PatchCustomerRequest();
        request.setPhone(JsonNullable.of("988777666"));

        CustomerChanges changes = mapper.toChanges(request);

        assertFalse(changes.email().provided());
        assertTrue(changes.phone().provided());
        assertEquals("988777666", changes.phone().value());
    }

    @Test
    void toChangesForPatchNullEmailClearsIt() {
        PatchCustomerRequest request = new PatchCustomerRequest();
        request.setEmail(JsonNullable.of(null));

        CustomerChanges changes = mapper.toChanges(request);

        assertTrue(changes.email().provided());
        assertNull(changes.email().value());
    }

    @Test
    void toChangesForPatchRejectsEmptyRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> mapper.toChanges(new PatchCustomerRequest()));
    }

    @Test
    void toChangesForPatchRejectsBothDetails() {
        PatchCustomerRequest request = new PatchCustomerRequest();
        request.setPersonalDetails(new PersonalDetails("Ana", "Perez"));
        request.setBusinessDetails(new BusinessDetails("ACME SAC"));

        assertThrows(IllegalArgumentException.class, () -> mapper.toChanges(request));
    }

    @Test
    void jsonNullableDistinguishesOmittedFromNullOnPatch() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .addModule(new JacksonConfig().jsonNullableModule())
                .build();

        PatchCustomerRequest omitted = jsonMapper.readValue(
                "{\"phone\":\"988777666\"}", PatchCustomerRequest.class);
        PatchCustomerRequest explicitNull = jsonMapper.readValue(
                "{\"email\":null}", PatchCustomerRequest.class);

        assertTrue(omitted.getEmail().isUndefined());
        assertTrue(explicitNull.getEmail().isPresent());
        assertNull(explicitNull.getEmail().get());

        CustomerChanges omittedChanges = mapper.toChanges(omitted);
        CustomerChanges nullChanges = mapper.toChanges(explicitNull);
        assertFalse(omittedChanges.email().provided());
        assertTrue(nullChanges.email().provided());
        assertNull(nullChanges.email().value());
    }
}
