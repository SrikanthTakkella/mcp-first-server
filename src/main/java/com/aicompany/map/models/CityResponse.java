package com.aicompany.map.models;

import com.fasterxml.jackson.annotation.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CityResponse {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("latitude")
    private double latitude;

    @JsonProperty("longitude")
    private double longitude;

    @JsonProperty("elevation")
    private int elevation;

    @JsonProperty("country")
    private String countrycode;

    @JsonProperty("admin1")
    private String admin1;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public int getElevation() { return elevation; }
    public void setElevation(int elevation) { this.elevation = elevation; }
    
    public String getCountrycode() { return countrycode; }
    public void setCountrycode(String countrycode) { this.countrycode = countrycode; }
    
    public String getAdmin1() { return admin1; }
    public void setAdmin1(String admin1) { this.admin1 = admin1; }
}
