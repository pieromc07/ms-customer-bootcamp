package com.nttdata.mscustomer.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nttdata.mscustomer.generated.dto.BusinessDetails;
import com.nttdata.mscustomer.generated.dto.PersonalDetails;
import com.nttdata.mscustomer.generated.dto.UpdateCustomerRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;

class UpdateCustomerRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void personalUpdateWithoutBusinessDetailsPassesBeanValidation() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setEmail(JsonNullable.of("ana.nueva@example.com"));
        request.setPhone(JsonNullable.of("99333444"));
        PersonalDetails personal = new PersonalDetails();
        personal.setFirstName("Ana");
        personal.setLastName("Torres Ruiz");
        request.setPersonalDetails(personal);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void businessUpdateWithoutPersonalDetailsPassesBeanValidation() {
        UpdateCustomerRequest request = new UpdateCustomerRequest();
        request.setBusinessDetails(new BusinessDetails());
        request.getBusinessDetails().setLegalName("Empresa Ejemplo S.A.C.");

        assertTrue(validator.validate(request).isEmpty());
    }
}
