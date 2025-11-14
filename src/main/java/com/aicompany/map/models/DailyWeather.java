package com.aicompany.map.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyWeather {
    
    @JsonProperty("time")
    private List<String> time;
    
    @JsonProperty("temperature_2m_mean")
    private List<Double> temperature2mMean;
    
    // Getters and setters
    public List<String> getTime() {
        return time;
    }
    
    public void setTime(List<String> time) {
        this.time = time;
    }
    
    public List<Double> getTemperature2mMean() {
        return temperature2mMean;
    }
    
    public void setTemperature2mMean(List<Double> temperature2mMean) {
        this.temperature2mMean = temperature2mMean;
    }
}