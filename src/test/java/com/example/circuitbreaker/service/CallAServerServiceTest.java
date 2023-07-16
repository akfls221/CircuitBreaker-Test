package com.example.circuitbreaker.service;

import com.example.circuitbreaker.client.CallSomeApiClient;
import com.example.circuitbreaker.fault_tolerance.BetweenAandBCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallAServerServiceTest {
    @MockBean
    private CallAServerService callAServerService;
    @Mock
    private CallSomeApiClient callSomeApiClientMock;
    @Mock
    private BetweenAandBCircuitBreaker betweenAandBCircuitBreakerMock;
    @Mock
    private CircuitBreaker circuitBreakerMock;


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