package com.nttdata.mscustomer.mapper;

import com.nttdata.mscustomer.document.CustomerDocument;
import com.nttdata.mscustomer.generated.dto.BusinessDetails;
import com.nttdata.mscustomer.generated.dto.CreateCustomerRequest;
import com.nttdata.mscustomer.generated.dto.Customer;
import com.nttdata.mscustomer.generated.dto.CustomerStatus;
import com.nttdata.mscustomer.generated.dto.PatchCustomerRequest;
import com.nttdata.mscustomer.generated.dto.PersonalDetails;
import com.nttdata.mscustomer.generated.dto.UpdateCustomerRequest;
import com.nttdata.mscustomer.model.ContactUpdate;
import com.nttdata.mscustomer.model.CustomerChanges;
import com.nttdata.mscustomer.model.CustomerProfile;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.model.DocumentType;
import java.time.ZoneOffset;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public com.nttdata.mscustomer.model.Customer toDomain(
            CreateCustomerRequest request) {
        if (request.getCustomerType() == null
                || request.getDocumentType() == null
                || request.getProfile() == null) {
            throw new IllegalArgumentException(
                    "Customer type, document type and profile are required");
        }

        return com.nttdata.mscustomer.model.Customer.create(
                CustomerType.valueOf(request.getCustomerType().getValue()),
                CustomerProfile.valueOf(request.getProfile().getValue()),
                DocumentType.valueOf(request.getDocumentType().getValue()),
                request.getDocumentNumber(),
                valueOf(request.getEmail()),
                valueOf(request.getPhone()),
                toPersonalDetails(request.getPersonalDetails()),
                toBusinessDetails(request.getBusinessDetails()));
    }

    public CustomerChanges toChanges(UpdateCustomerRequest request) {
        if ((request.getPersonalDetails() == null) == (request.getBusinessDetails() == null)) {
            throw new IllegalArgumentException(
                    "Exactly one of personalDetails or businessDetails is required");
        }

        return new CustomerChanges(
                ContactUpdate.set(valueOf(request.getEmail())),
                ContactUpdate.set(valueOf(request.getPhone())),
                toPersonalDetails(request.getPersonalDetails()),
                toBusinessDetails(request.getBusinessDetails()));
    }

    public CustomerChanges toChanges(PatchCustomerRequest request) {
        if (request.getPersonalDetails() != null && request.getBusinessDetails() != null) {
            throw new IllegalArgumentException(
                    "Only one of personalDetails or businessDetails can be patched");
        }

        ContactUpdate email = toContactUpdate(request.getEmail());
        ContactUpdate phone = toContactUpdate(request.getPhone());
        if (!email.provided() && !phone.provided()
                && request.getPersonalDetails() == null
                && request.getBusinessDetails() == null) {
            throw new IllegalArgumentException("At least one field must be provided");
        }

        return new CustomerChanges(
                email,
                phone,
                toPersonalDetails(request.getPersonalDetails()),
                toBusinessDetails(request.getBusinessDetails()));
    }

    public com.nttdata.mscustomer.model.CustomerStatus toDomainStatus(
            CustomerStatus status) {
        return status == null
                ? null
                : com.nttdata.mscustomer.model.CustomerStatus.valueOf(status.getValue());
    }

    public CustomerType toDomainCustomerType(
            com.nttdata.mscustomer.generated.dto.CustomerType customerType) {
        return customerType == null ? null : CustomerType.valueOf(customerType.getValue());
    }

    public CustomerProfile toDomainProfile(
            com.nttdata.mscustomer.generated.dto.CustomerProfile profile) {
        return profile == null ? null : CustomerProfile.valueOf(profile.getValue());
    }

    private static <T> T valueOf(JsonNullable<T> value) {
        return value == null ? null : value.orElse(null);
    }

    private static ContactUpdate toContactUpdate(JsonNullable<String> value) {
        if (value == null || value.isUndefined()) {
            return ContactUpdate.KEEP;
        }
        return ContactUpdate.set(value.orElse(null));
    }

    private static com.nttdata.mscustomer.model.PersonalDetails toPersonalDetails(
            PersonalDetails details) {
        return details == null
                ? null
                : new com.nttdata.mscustomer.model.PersonalDetails(
                        details.getFirstName(),
                        details.getLastName());
    }

    private static com.nttdata.mscustomer.model.BusinessDetails toBusinessDetails(
            BusinessDetails details) {
        return details == null
                ? null
                : new com.nttdata.mscustomer.model.BusinessDetails(
                        details.getLegalName(),
                        valueOf(details.getTradeName()));
    }

    public CustomerDocument toDocument(
            com.nttdata.mscustomer.model.Customer customer) {
        CustomerDocument document = new CustomerDocument();
        document.setId(customer.getId());
        document.setCustomerType(customer.getCustomerType());
        document.setProfile(customer.getProfile());
        document.setDocumentType(customer.getDocumentType());
        document.setDocumentNumber(customer.getDocumentNumber());
        document.setEmail(customer.getEmail());
        document.setPhone(customer.getPhone());
        document.setPasswordHash(customer.getPasswordHash());
        document.setStatus(customer.getStatus());
        document.setPersonalDetails(customer.getPersonalDetails());
        document.setBusinessDetails(customer.getBusinessDetails());
        document.setCreatedAt(customer.getCreatedAt());
        document.setUpdatedAt(customer.getUpdatedAt());
        return document;
    }

    public com.nttdata.mscustomer.model.Customer toDomain(
            CustomerDocument document) {
        com.nttdata.mscustomer.model.Customer customer = new com.nttdata.mscustomer.model.Customer(
                document.getId(),
                document.getCustomerType(),
                document.getProfile(),
                document.getDocumentType(),
                document.getDocumentNumber(),
                document.getEmail(),
                document.getPhone(),
                document.getStatus(),
                document.getPersonalDetails(),
                document.getBusinessDetails(),
                document.getCreatedAt(),
                document.getUpdatedAt());
        customer.setPasswordHash(document.getPasswordHash());
        return customer;
    }

    public Customer toResponse(
            com.nttdata.mscustomer.model.Customer customer) {
        Customer response = new Customer();
        response.setId(customer.getId());
        response.setCustomerType(Customer.CustomerTypeEnum
                .valueOf(customer.getCustomerType().name()));
        response.setDocumentType(Customer.DocumentTypeEnum
                .valueOf(customer.getDocumentType().name()));
        response.setDocumentNumber(customer.getDocumentNumber());
        response.setEmail(JsonNullable.of(customer.getEmail()));
        response.setPhone(JsonNullable.of(customer.getPhone()));
        response.setProfile(Customer.ProfileEnum
                .valueOf(customer.getProfile().name()));
        response.setStatus(CustomerStatus.valueOf(customer.getStatus().name()));
        response.setCreatedAt(customer.getCreatedAt().atOffset(ZoneOffset.UTC));
        response.setUpdatedAt(customer.getUpdatedAt().atOffset(ZoneOffset.UTC));

        if (customer.getCustomerType() == CustomerType.PERSONAL) {
            response.setPersonalDetails(new PersonalDetails(
                    customer.getPersonalDetails().firstName(),
                    customer.getPersonalDetails().lastName()));
        } else {
            BusinessDetails businessDetails = new BusinessDetails(
                    customer.getBusinessDetails().legalName());
            businessDetails.setTradeName(JsonNullable.of(customer.getBusinessDetails().tradeName()));
            response.setBusinessDetails(businessDetails);
        }
        return response;
    }
}
