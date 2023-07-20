package com.example.aserver.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecuteApi {

    @GetMapping("/execute")
    public String test() {
        return "this is A server";
    }
}
