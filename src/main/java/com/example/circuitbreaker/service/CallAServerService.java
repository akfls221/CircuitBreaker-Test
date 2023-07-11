package com.example.circuitbreaker.service;

import com.example.circuitbreaker.client.CallSomeApiClient;
import com.example.circuitbreaker.fault_tolerance.BetweenAandBCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.vavr.control.Try;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallAServerService {
    private final CallSomeApiClient apiClient;
    private final BetweenAandBCircuitBreaker betweenAandBCircuitBreaker;

    public void callAServer() {
        CircuitBreaker circuitBreaker = betweenAandBCircuitBreaker.addCircuitBreaker("callA");

        try {
            Supplier<String> decorateSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, apiClient::callAServerApi);
            String apiResponse = Try.ofSupplier(decorateSupplier).recover(throwable -> callA_1Server()).get();
            log.info("api result : {}", apiResponse);
        } catch (CallNotPermittedException e) {
            log.warn("service is block because circuitBreaker block this request");
        } catch (Exception e) {
            log.error("UnKnown Exception occur", e);
        }
    }

    private String callA_1Server() {
        return "fallback method running";
    }
}
