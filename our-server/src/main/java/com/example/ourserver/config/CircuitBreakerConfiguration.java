package com.example.ourserver.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.ResourceAccessException;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
public class CircuitBreakerConfiguration {

    @Bean
     CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.of(configurationCircuitBreaker());
    }

    private CircuitBreakerConfig configurationCircuitBreaker() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(40) //실패율 임계값(밴분율 단위)
                .waitDurationInOpenState(Duration.ofMillis(10000))   //Open -> half-open으로 전환되기 전에 대기시간
                .permittedNumberOfCallsInHalfOpenState(3)           //half-open시에 허용되는 호출 수
                .slidingWindowSize(10)                               //호출 결과를 기록하는 데 사용되는 슬라이딩 윈도우 크기
                .recordExceptions(Exception.class)    //실패로 기록되어 실패율이 증가하는 예외 목록
                .build();
    }


}
