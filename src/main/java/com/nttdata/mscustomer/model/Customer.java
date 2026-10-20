package com.nttdata.mscustomer.model;

import com.nttdata.mscustomer.exception.InactiveCustomerException;
import lombok.Data;
import java.time.Instant;
import java.util.Set;
import java.util.regex.Pattern;

@Data
public class Customer {

    private static final Set<DocumentType> PERSONAL_DOCUMENT_TYPES =
            Set.of(DocumentType.DNI, DocumentType.CE, DocumentType.PASSPORT);
    private static final Set<CustomerProfile> PERSONAL_PROFILES =
            Set.of(CustomerProfile.STANDARD, CustomerProfile.VIP);
    private static final Set<CustomerProfile> BUSINESS_PROFILES =
            Set.of(CustomerProfile.STANDARD, CustomerProfile.PYME);
    private static final Pattern DNI_PATTERN = Pattern.compile("^[0-9]{8}$");
    private static final Pattern RUC_PATTERN = Pattern.compile("^(10|15|16|17|20)[0-9]{9}$");

    private final String id;
    private final CustomerType customerType;
    private CustomerProfile profile;
    private DocumentType documentType;
    private String documentNumber;
    private String email;
    private String phone;
    private String passwordHash;
    private CustomerStatus status;
    private PersonalDetails personalDetails;
    private BusinessDetails businessDetails;
    private final Instant createdAt;
    private Instant updatedAt;

    public Customer(
            String id,
            CustomerType customerType,
            CustomerProfile profile,
            DocumentType documentType,
            String documentNumber,
            String email,
            String phone,
            CustomerStatus status,
            PersonalDetails personalDetails,
            BusinessDetails businessDetails,
            Instant createdAt,
            Instant updatedAt
    ) {
        validateDetails(customerType, personalDetails, businessDetails);

        this.id = id;
        this.customerType = customerType;
        this.profile = profile;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.email = email;
        this.phone = phone;
        this.status = status;
        this.personalDetails = personalDetails;
        this.businessDetails = businessDetails;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Customer create(
            CustomerType customerType,
            CustomerProfile profile,
            DocumentType documentType,
            String documentNumber,
            String email,
            String phone,
            PersonalDetails personalDetails,
            BusinessDetails businessDetails
    ) {
        validateDetails(customerType, personalDetails, businessDetails);
        validateCreationRules(customerType, profile, documentType, documentNumber);

        Instant now = Instant.now();
        return new Customer(
                null,
                customerType,
                profile,
                documentType,
                documentNumber,
                email,
                phone,
                CustomerStatus.ACTIVE,
                personalDetails,
                businessDetails,
                now,
                now
        );
    }

    public void applyChanges(CustomerChanges changes, Instant now) {
        ensureActive();

        PersonalDetails newPersonalDetails = changes.personalDetails() != null
                ? changes.personalDetails()
                : this.personalDetails;
        BusinessDetails newBusinessDetails = changes.businessDetails() != null
                ? changes.businessDetails()
                : this.businessDetails;
        validateDetails(customerType, newPersonalDetails, newBusinessDetails);

        if (changes.email().provided()) {
            this.email = changes.email().value();
        }
        if (changes.phone().provided()) {
            this.phone = changes.phone().value();
        }
        this.personalDetails = newPersonalDetails;
        this.businessDetails = newBusinessDetails;
        this.updatedAt = now;
    }

    public boolean deactivate(Instant now) {
        if (status == CustomerStatus.INACTIVE) {
            return false;
        }
        status = CustomerStatus.INACTIVE;
        updatedAt = now;
        return true;
    }

    public void assignPasswordHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new IllegalArgumentException("Password hash is required");
        }
        this.passwordHash = hash;
    }

    private void ensureActive() {
        if (status != CustomerStatus.ACTIVE) {
            throw new InactiveCustomerException(
                    "Customer " + id + " is inactive and cannot be modified");
        }
    }

    static void validateDetails(
            CustomerType customerType,
            PersonalDetails personalDetails,
            BusinessDetails businessDetails
    ) {
        if (customerType == null) {
            throw new IllegalArgumentException("Customer type is required");
        }

        if (customerType == CustomerType.PERSONAL
                && (personalDetails == null || businessDetails != null)) {
            throw new IllegalArgumentException(
                    "A PERSONAL customer must have personalDetails and no businessDetails"
            );
        }

        if (customerType == CustomerType.BUSINESS
                && (businessDetails == null || personalDetails != null)) {
            throw new IllegalArgumentException(
                    "A BUSINESS customer must have businessDetails and no personalDetails"
            );
        }
    }

    private static void validateCreationRules(
            CustomerType customerType,
            CustomerProfile profile,
            DocumentType documentType,
            String documentNumber
    ) {
        if (profile == null || documentType == null) {
            throw new IllegalArgumentException("Document type and profile are required");
        }

        if (customerType == CustomerType.PERSONAL) {
            if (!PERSONAL_DOCUMENT_TYPES.contains(documentType)) {
                throw new IllegalArgumentException(
                        "A PERSONAL customer must have document type DNI, CE or PASSPORT");
            }
            if (!PERSONAL_PROFILES.contains(profile)) {
                throw new IllegalArgumentException(
                        "A PERSONAL customer must have profile STANDARD or VIP");
            }
        } else {
            if (documentType != DocumentType.RUC) {
                throw new IllegalArgumentException(
                        "A BUSINESS customer must have document type RUC");
            }
            if (!BUSINESS_PROFILES.contains(profile)) {
                throw new IllegalArgumentException(
                        "A BUSINESS customer must have profile STANDARD or PYME");
            }
        }

        if (documentType == DocumentType.DNI
                && (documentNumber == null || !DNI_PATTERN.matcher(documentNumber).matches())) {
            throw new IllegalArgumentException("DNI must have exactly 8 digits");
        }
        if (documentType == DocumentType.RUC
                && (documentNumber == null || !RUC_PATTERN.matcher(documentNumber).matches())) {
            throw new IllegalArgumentException(
                    "RUC must have 11 digits and start with 10, 15, 16, 17 or 20");
        }
    }
}

