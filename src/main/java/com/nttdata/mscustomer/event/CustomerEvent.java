package com.nttdata.mscustomer.event;

import com.nttdata.mscustomer.model.Customer;
import com.nttdata.mscustomer.model.CustomerProfile;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.model.DocumentType;
import java.time.Instant;
import java.util.UUID;

public record CustomerEvent(
        String eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        String customerId,
        CustomerType customerType,
        DocumentType documentType,
        String documentNumber,
        CustomerProfile profile,
        CustomerStatus status) {

    public static final int SCHEMA_VERSION = 1;

    public enum Type {
        CREATED("customer.created"),
        UPDATED("customer.updated"),
        DEACTIVATED("customer.deactivated");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public static CustomerEvent of(Type type, Customer customer) {
        return new CustomerEvent(
                UUID.randomUUID().toString(),
                type.value(),
                SCHEMA_VERSION,
                Instant.now(),
                customer.getId(),
                customer.getCustomerType(),
                customer.getDocumentType(),
                customer.getDocumentNumber(),
                customer.getProfile(),
                customer.getStatus());
    }
}
