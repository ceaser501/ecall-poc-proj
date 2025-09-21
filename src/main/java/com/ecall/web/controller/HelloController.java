package com.ecall.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HelloController {
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        return Map.of("status", "OK", "service", "ecall-web");
    }
}