package com.example.circuitbreaker.service;

import com.example.circuitbreaker.fault_tolerance.BetweenAandBCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class CallAServerService {

    private static final String A_SERVER_URL = "http://localhost:9090/execute";
//    private static final String A_SERVER_URL = "www.naver.com";
    private static final String B_SERVER_URL = "localhost:9091/execute";
    private final RestTemplate restTemplate;
    private final BetweenAandBCircuitBreaker betweenAandBCircuitBreaker;

    public void callAServer() {
        CircuitBreaker circuitBreaker = betweenAandBCircuitBreaker.addCircuitBreaker("callA");

        try {
            circuitBreaker.executeSupplier(() -> restTemplate.getForObject(A_SERVER_URL, String.class));
        } catch (CallNotPermittedException e) {
            log.warn("service is block because circuitBreaker state is OPEN");
        } catch (Exception e) {
            log.error("ERROR!!!");
        }
    }

//    public void callBServer() {
//        restTemplate.getForObject();
//    }


}
