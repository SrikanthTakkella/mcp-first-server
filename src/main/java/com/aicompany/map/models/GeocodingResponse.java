package com.aicompany.map.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GeocodingResponse {
    @JsonProperty("results")
    private List<CityResponse> results;
    
    public List<CityResponse> getResults() {
        return results;
    }
    
    public void setResults(List<CityResponse> results) {
        this.results = results;
    }
}