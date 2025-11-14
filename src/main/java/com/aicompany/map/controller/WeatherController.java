package com.aicompany.map.controller;

import com.aicompany.map.service.WeatherService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class WeatherController {
    
    private final WeatherService weatherService;
    
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }
    
    @GetMapping("/weather")
    public Mono<String> getWeather(
        @RequestParam String city, 
        @RequestParam String country
    ) {
        return Mono.fromCallable(() -> weatherService.getWeatherInfo(city, country))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
    
    @GetMapping("/test")
    public String test() {
        return "Simple test works! Your MCP server is responding.";
    }
    
    @GetMapping("/health")
    public String health() {
        return "Weather MCP Server is running! ✅";
    }
}