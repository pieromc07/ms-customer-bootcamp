package com.nttdata.mscustomer.event;

import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class CustomerEventPublisher {

    public static final String TOPIC = "customer.events";

    private static final Logger LOG = LoggerFactory.getLogger(CustomerEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CustomerEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Completable publish(CustomerEvent event) {
        return Completable.defer(() -> {
                    String payload = objectMapper.writeValueAsString(event);
                    CompletableFuture<?> sent = kafkaTemplate.send(TOPIC, event.customerId(), payload);
                    return Completable.create(emitter -> sent.whenComplete((result, error) -> {
                        if (error == null) {
                            emitter.onComplete();
                        } else {
                            emitter.onError(error);
                        }
                    }));
                })
                .doOnError(error -> LOG.error(
                        "Could not publish {} for customer {}",
                        event.eventType(), event.customerId(), error))
                .onErrorComplete();
    }
}
