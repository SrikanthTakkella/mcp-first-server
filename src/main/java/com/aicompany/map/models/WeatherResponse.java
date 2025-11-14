package com.aicompany.map.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {
    
    @JsonProperty("daily")
    private DailyWeather daily;
    
    // Getters and setters
    public DailyWeather getDaily() {
        return daily;
    }
    
    public void setDaily(DailyWeather daily) {
        this.daily = daily;
    }
}
