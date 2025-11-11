package com.ecall.step2.s2locationextraction.controller;

import com.ecall.step2.s2locationextraction.dto.LocationDto;
import com.ecall.step2.s2locationextraction.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/step2/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping("/geocode")
    public LocationDto geocode(@RequestParam String address) {
        return mapService.getGeocode(address);
    }

    @GetMapping("/route")
    public List<LocationDto> route(@RequestParam double startLat, @RequestParam double startLng,
                                     @RequestParam double endLat, @RequestParam double endLng) {
        return mapService.getRoute(startLat, startLng, endLat, endLng);
    }
}
