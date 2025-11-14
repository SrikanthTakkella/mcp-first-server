package com.aicompany.map.models;

public class DailyForecast {
    private String date;
    private Double temperature;
    
    public DailyForecast(String date, Double temperature) {
        this.date = date;
        this.temperature = temperature;
    }
    
    public String getDate() {
        return date;
    }
    
    public void setDate(String date) {
        this.date = date;
    }
    
    public Double getTemperature() {
        return temperature;
    }
    
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }
    
    @Override
    public String toString() {
        return date + ": " + temperature + "°C";
    }
}