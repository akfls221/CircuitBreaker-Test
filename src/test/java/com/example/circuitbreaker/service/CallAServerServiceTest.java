package com.example.circuitbreaker.service;

import com.example.circuitbreaker.client.CallSomeApiClient;
import com.example.circuitbreaker.fault_tolerance.BetweenAandBCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallAServerServiceTest {
    @Mock
    private CallAServerService callAServerService;
    @Mock
    private CallSomeApiClient callSomeApiClientMock;
    @Mock
    private BetweenAandBCircuitBreaker betweenAandBCircuitBreakerMock;
    private CircuitBreaker circuitBreakerMock;


    @BeforeEach
    void init() {
        CircuitBreakerConfig defaultCircuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(10) //실패율 임계값(밴분율 단위)
                .waitDurationInOpenState(Duration.ofMillis(1000))   //Open -> half-open으로 전환되기 전에 대기시간
                .permittedNumberOfCallsInHalfOpenState(3)           //half-open시에 허용되는 호출 수
                .slowCallDurationThreshold(Duration.ofMillis(1000)) //통신이 느린것으로 간주되는 기간 임계값
                .slidingWindowSize(10)                               //호출 결과를 기록하는 데 사용되는 슬라이딩 윈도우 크기
                .recordExceptions(IOException.class, TimeoutException.class, ResourceAccessException.class, HttpClientErrorException.class)    //실패로 기록되어 실패율이 증가하는 예외 목록
                .build();

        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.of(defaultCircuitBreakerConfig);
        circuitBreakerMock = circuitBreakerRegistry.circuitBreaker("callA");
    }

    @Test
    void success_case() {
        // Given
        callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
        when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);
        when(callSomeApiClientMock.callAServerApi()).thenReturn("success");

        // When
        String result = callAServerService.callAServer();

        // Then
        assertThat(result).isEqualTo("success");
    }

    @Test
    void fail_case() {
        // Given
        callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
        when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);
        when(callSomeApiClientMock.callAServerApi()).thenThrow(RuntimeException.class);

        // When
        String result = callAServerService.callAServer();

        // Then
        assertThat(result).isEqualTo("fallback method running");
    }

}