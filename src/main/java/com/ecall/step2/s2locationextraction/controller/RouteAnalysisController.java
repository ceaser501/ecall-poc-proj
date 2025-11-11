package com.ecall.step2.s2locationextraction.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/step2/route-analysis")
public class RouteAnalysisController {

    @GetMapping("/calculate")
    public String calculateRoute(@RequestParam("start") String start, @RequestParam("end") String end) {
        // TODO: Implement route calculation using Naver Maps API
        return "Route calculation initiated from " + start + " to " + end;
    }
}
