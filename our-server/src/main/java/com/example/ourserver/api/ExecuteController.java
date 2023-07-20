package com.example.ourserver.api;

import com.example.ourserver.service.CallAServerService;
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
    public String executeCall() {
        return callAServerService.callAServer();
    }
}
