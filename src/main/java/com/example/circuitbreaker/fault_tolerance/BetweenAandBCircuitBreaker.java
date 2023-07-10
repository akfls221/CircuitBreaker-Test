package com.example.circuitbreaker.fault_tolerance;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BetweenAandBCircuitBreaker {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private static CircuitBreaker circuitBreaker;

    public synchronized CircuitBreaker addCircuitBreaker(String entryName) {
        if (circuitBreaker == null) {
            addRegistryEvent();
            circuitBreaker = circuitBreakerRegistry.circuitBreaker(entryName);

            circuitBreaker.getEventPublisher()
                    .onSuccess(event -> log.info("success call A Method"))
                    .onError(event -> log.error("fail call A Method"))
                    .onIgnoredError(event -> log.info("ignore Exception occurred"))
                    .onReset(event -> log.info("state is reset"))
                    .onStateTransition(event -> log.info("change state result : {}", event.getStateTransition()));

            return circuitBreaker;
        }
        return circuitBreaker;
    }

    private void addRegistryEvent() {
        circuitBreakerRegistry.getEventPublisher()
                .onEntryAdded(entryAddedEvent -> {
                    CircuitBreaker addedEntry = entryAddedEvent.getAddedEntry();
                    log.info("CircuitBreaker {} added", addedEntry.getName());
                });
    }




}
