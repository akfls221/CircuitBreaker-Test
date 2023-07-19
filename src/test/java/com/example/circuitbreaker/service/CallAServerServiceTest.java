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

@DisplayName("서킷브레이커 테스트")
@ExtendWith(MockitoExtension.class)
class CallAServerServiceTest {
    @MockBean
    CallAServerService callAServerService;
    @Mock
    CallSomeApiClient callSomeApiClientMock;
    @Mock
    BetweenAandBCircuitBreaker betweenAandBCircuitBreakerMock;
    @Mock
    CircuitBreaker circuitBreakerMock;

    @BeforeEach
    void init() {
        CircuitBreakerConfig circuitBreakerConfigMock = CircuitBreakerConfig.custom()
                .failureRateThreshold(40) //실패율 임계값(밴분율 단위)
                .waitDurationInOpenState(Duration.ofMillis(1000))   //Open -> half-open으로 전환되기 전에 대기시간
                .permittedNumberOfCallsInHalfOpenState(3)           //half-open시에 허용되는 호출 수
                .slidingWindowSize(10)                               //호출 결과를 기록하는 데 사용되는 슬라이딩 윈도우 크기
                .recordExceptions(RuntimeException.class)    //실패로 기록되어 실패율이 증가하는 예외 목록
                .build();

        CircuitBreakerRegistry circuitBreakerRegistryMock = CircuitBreakerRegistry.of(circuitBreakerConfigMock);
        circuitBreakerMock = circuitBreakerRegistryMock.circuitBreaker("testCircuitBreaker");
    }

    @Nested
    @DisplayName("성공한 케이스 - CLOSE")
    class SuccessTest{

        @Test
        void 한번의_호출로_성공한_케이스() {
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

        @Test
        void 임계점_직전까지의_호출로_성공한_케이스() {
            // Given
            callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
            when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);

            int expectedThrowCount = 3;
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
            for (int i = 0; i < 10; i++) {
                callAServerService.callAServer();
            }

            // Then
            assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
            assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
            assertThat(circuitBreakerMock.getMetrics().getFailureRate()).isEqualTo(30.0F);
            assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(callAServerService.callAServer()).isEqualTo("success");
        }
    }

    @Nested
    @DisplayName("실패 케이스 - OPEN")
    class FailTest {
        @Test
        void 모든_호출이_실패한_케이스() {
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
            assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isEqualTo(10);
            assertThat(circuitBreakerMock.getMetrics().getFailureRate()).isEqualTo(100.0F);
            assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(callAServerService.callAServer()).isEqualTo("fallback method running");
        }

        @Test
        void 임계점_달성시_CLOSE_에서_OPEN_상태가된다 () {
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
            for (int i = 0; i < 10; i++) {
                callAServerService.callAServer();
            }

            // Then
            assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
            assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isEqualTo(4);
            assertThat(circuitBreakerMock.getMetrics().getFailureRate()).isEqualTo(40.0F);
            assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(callAServerService.callAServer()).isEqualTo("fallback method running");
        }
    }

    @Nested
    @DisplayName("반개방 케이스 - HALF_OPEN")
    class HalfOpenTest {
        @Test
        void 정해진_임계점만큼_실패후_1초가_지나면_HALF_OPEN_상태가된다() throws InterruptedException {
            // Given
            callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
            when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);

            int expectedThrowCount = 5;
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
            for (int i = 0; i < 12; i++) {
                callAServerService.callAServer();
                if (i == 10) {
                    Thread.sleep(1000);
                }
            }
            // Then
            assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
            assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isZero();
            assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
        }

        @Test
        void HALF_OPEN_상태에서_3번의_요청이_실패하면_OPEN_상태가된다() throws InterruptedException {
            // Given
            callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
            when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);

            int expectedThrowCount = 5;
            AtomicInteger throwCount = new AtomicInteger();

            Answer<String> throwExceptionAnswer = invocation -> {
                throwCount.getAndIncrement();
                if (throwCount.get() <= expectedThrowCount) {
                    throw new RuntimeException();
                }

                if (throwCount.get() > 10) {
                    throw new RuntimeException();
                }
                return "success";
            };

            when(callSomeApiClientMock.callAServerApi()).thenAnswer(throwExceptionAnswer);

            // When
            for (int i = 0; i < 14; i++) {
                callAServerService.callAServer();
                if (i == 10) {
                    Thread.sleep(1000);
                }
            }

            // Then
            assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
            assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
            assertThat(circuitBreakerMock.getMetrics().getFailureRate()).isEqualTo(100.0F);
            assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        void HALF_OPEN_상태에서_3번이상_정상호출시_CLOSE_상태가된다 () throws InterruptedException {
            // Given
            callAServerService = new CallAServerService(callSomeApiClientMock, betweenAandBCircuitBreakerMock);
            when(betweenAandBCircuitBreakerMock.addCircuitBreaker(anyString())).thenReturn(circuitBreakerMock);

            int expectedThrowCount = 5;
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
            for (int i = 0; i < 14; i++) {
                callAServerService.callAServer();
                if (i == 10) {
                    Thread.sleep(1000);
                }
            }

            // Then
            assertThat(circuitBreakerMock.getName()).isEqualTo("testCircuitBreaker");
            assertThat(circuitBreakerMock.getMetrics().getNumberOfFailedCalls()).isZero();
            assertThat(circuitBreakerMock.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(callAServerService.callAServer()).isEqualTo("success");
        }
    }
}