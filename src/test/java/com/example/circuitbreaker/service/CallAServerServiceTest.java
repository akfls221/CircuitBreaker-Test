package com.example.circuitbreaker.service;

import com.example.circuitbreaker.client.CallSomeApiClient;
import com.example.circuitbreaker.fault_tolerance.BetweenAandBCircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("서킷브레이커 테스트")
class CallAServerServiceTest {
    @MockBean
    private static CallAServerService callAServerService;
    @Mock
    private static CallSomeApiClient callSomeApiClientMock;
    @Mock
    private static BetweenAandBCircuitBreaker betweenAandBCircuitBreakerMock;
    @Mock
    private static CircuitBreaker circuitBreakerMock;

    @BeforeEach
    void init() {
        CircuitBreakerConfig circuitBreakerConfigMock = CircuitBreakerConfig.custom()
                .failureRateThreshold(30) //실패율 임계값(밴분율 단위)
                .waitDurationInOpenState(Duration.ofMillis(1000))   //Open -> half-open으로 전환되기 전에 대기시간
                .permittedNumberOfCallsInHalfOpenState(3)           //half-open시에 허용되는 호출 수
                .slidingWindowSize(5)                               //호출 결과를 기록하는 데 사용되는 슬라이딩 윈도우 크기
                .recordExceptions(RuntimeException.class)    //실패로 기록되어 실패율이 증가하는 예외 목록
                .build();

        CircuitBreakerRegistry circuitBreakerRegistryMock = CircuitBreakerRegistry.of(circuitBreakerConfigMock);
        circuitBreakerMock = circuitBreakerRegistryMock.circuitBreaker("testCircuitBreaker");
    }

    @Nested
    @DisplayName("성공한 케이스")
    static class SuccessTest extends CallAServerServiceTest{

        @Test
        @DisplayName("1번의 호출로 성공한 케이스")
        void success_case() {
            // Given
            callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
            when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);
            when(callSomeApiClientMock.callAServerApi()).thenReturn("success");

            // When
            String result = callAServerService.callAServer();

            // Then
            assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
            assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isZero();
            assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(result).isEqualTo("success");
        }
    }

    @Test
    @DisplayName("서킷브레이커_성공테스트")
    void fail_case() {
        // Given
        callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
        when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);
        when(callSomeApiClientMock.callAServerApi()).thenThrow(RuntimeException.class);

        // When
        String result = callAServerService.callAServer();

        // Then
        assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
        assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(result).isEqualTo("fallback method running");
    }

    @Test
    @DisplayName("서킷브레이커_")
    void fail_case2() {
        // Given
        callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
        when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);
        when(callSomeApiClientMock.callAServerApi()).thenThrow(RuntimeException.class);

        // When
        for (int i = 0; i < 10; i++) {
            callAServerService.callAServer();
        }

        // Then
        assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
        assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isEqualTo(5);
        assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void fail_case3() throws InterruptedException {
        // Given
        callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
        when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);

        int expectedThrowCount = 4;
        AtomicInteger throwCount = new AtomicInteger();

        Answer<String> throwExceptionAnswer = invocation -> {
            throwCount.getAndIncrement();
            if (throwCount.get() <= expectedThrowCount) {
                throw new RuntimeException();
            }
            return "success";
        };

        when(callSomeApiClientMock.callAServerApi()).thenAnswer(throwExceptionAnswer);

        // When
        for (int i = 0; i < 6; i++) {
            callAServerService.callAServer();
            if (i == 4) {
                Thread.sleep(1000);
            }
        }

        // Then
        assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
        assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    void fail_case4() throws InterruptedException {
        // Given
        callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
        when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);

        int expectedThrowCount = 4;
        AtomicInteger throwCount = new AtomicInteger();

        Answer<String> throwExceptionAnswer = invocation -> {
            throwCount.getAndIncrement();
            if (throwCount.get() <= expectedThrowCount) {
                throw new RuntimeException();
            }
            return "success";
        };

        when(callSomeApiClientMock.callAServerApi()).thenAnswer(throwExceptionAnswer);

        // When
        for (int i = 0; i < 9; i++) {
            callAServerService.callAServer();
            if (i == 4) {
                Thread.sleep(1000);
            }
        }

        // Then
        assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
        assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

}