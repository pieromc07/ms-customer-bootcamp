package com.nttdata.mscustomer.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nttdata.mscustomer.model.CustomerProfile;
import com.nttdata.mscustomer.model.CustomerStatus;
import com.nttdata.mscustomer.model.CustomerType;
import com.nttdata.mscustomer.model.DocumentType;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CustomerEventPublisherTest {

    private static final String ID = "665f1c2e8a1b2c3d4e5f6a7b";

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private CustomerEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new CustomerEventPublisher(kafkaTemplate, JsonMapper.builder().build());
    }

    @Test
    void publishSendsVersionedEventKeyedByCustomerId() {
        when(kafkaTemplate.send(eq(CustomerEventPublisher.TOPIC), eq(ID), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event()).test().assertComplete();

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(CustomerEventPublisher.TOPIC), eq(ID), payload.capture());
        assertTrue(payload.getValue().contains("\"eventType\":\"customer.created\""));
        assertTrue(payload.getValue().contains("\"schemaVersion\":1"));
        assertTrue(payload.getValue().contains("\"status\":\"ACTIVE\""));
    }

    @Test
    void publishNeverIncludesPasswordData() {
        when(kafkaTemplate.send(eq(CustomerEventPublisher.TOPIC), eq(ID), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publish(event()).test().assertComplete();

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(CustomerEventPublisher.TOPIC), eq(ID), payload.capture());
        assertFalse(payload.getValue().toLowerCase().contains("password"));
    }

    @Test
    void publishCompletesEvenWhenBrokerFails() {
        when(kafkaTemplate.send(eq(CustomerEventPublisher.TOPIC), eq(ID), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker down")));

        publisher.publish(event()).test().assertComplete();
    }

    @Test
    void publishDoesNotSendUntilSubscribed() {
        publisher.publish(event());

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    private static CustomerEvent event() {
        return new CustomerEvent(
                "9b2f6c1e-0000-4000-8000-000000000001",
                "customer.created",
                1,
                Instant.parse("2024-01-01T00:00:00Z"),
                ID,
                CustomerType.PERSONAL,
                DocumentType.DNI,
                "12345678",
                CustomerProfile.STANDARD,
                CustomerStatus.ACTIVE);
    }
}
