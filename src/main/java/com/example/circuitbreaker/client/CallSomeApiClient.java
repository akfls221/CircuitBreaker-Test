package com.example.circuitbreaker.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CallSomeApiClient {

    private static final String A_SERVER_URL = "http://localhost:9090/execute";
    private static final String B_SERVER_URL = "http://localhost:9091/execute";
    private final RestTemplate restTemplate;

    public String callAServerApi() {
        try {
            log.info("start call A server");
            return restTemplate.getForObject(A_SERVER_URL, String.class);
        } catch (Exception e) {
            log.error("call A Server failed");
            throw e;
        }
    }

    public String callBServerApi() {
        try {
            return restTemplate.getForObject(B_SERVER_URL, String.class);
        } catch (Exception e) {
            log.error("call A Server failed");
            throw e;
        }
    }


}

