package com.example.aserver.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecuteApi {

    private static int failCount;

    @GetMapping("/execute")
    public String test() {
        if (failCount < 5) {
            failCount++;
            throw new RuntimeException();
        }
        return "this is A server";
    }
}
