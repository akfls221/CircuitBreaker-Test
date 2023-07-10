package com.example.circuitbreaker.api;

import com.example.circuitbreaker.service.CallAServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ExecuteController {

    private final CallAServerService callAServerService;

    @GetMapping
    public void executeCall() {
//        for (int i = 0; i < 20; i++) {
            callAServerService.callAServer();
//        }
    }
}
