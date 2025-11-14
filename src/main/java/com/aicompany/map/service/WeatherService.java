package com.aicompany.map.service;

import com.aicompany.map.models.CityResponse;
import com.aicompany.map.models.GeocodingResponse;
import com.aicompany.map.models.WeatherResponse;
import com.aicompany.map.models.DailyForecast;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.ai.tool.annotation.Tool;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class WeatherService {

    private final WebClient geocodingClient;
    private final WebClient weatherClient;
    
    public WeatherService() {
        this.geocodingClient = WebClient.builder()
            .baseUrl("https://geocoding-api.open-meteo.com")
            .build();
            
        this.weatherClient = WebClient.builder()
            .baseUrl("https://api.open-meteo.com")
            .build();
    }

    @Tool(description = "Get temperature forecast for a city for the next days in celsius")
    public String getWeatherInfo(String name, String countrycode) {
        try {
            GeocodingResponse geocodingResponse = geocodingClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search")
                            .queryParam("name", name)
                            .queryParam("countryCode", countrycode)
                            .queryParam("language", "en")
                            .queryParam("format", "json")
                            .build())
                    .retrieve()
                    .bodyToMono(GeocodingResponse.class)
                    .block();

            if (geocodingResponse == null || geocodingResponse.getResults() == null || geocodingResponse.getResults().isEmpty()) {
                return "No results found for " + name + ", " + countrycode;
            }

            CityResponse firstResult = geocodingResponse.getResults().get(0);

            WeatherResponse weatherResponse = weatherClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/forecast")
                            .queryParam("latitude", firstResult.getLatitude())
                            .queryParam("longitude", firstResult.getLongitude())
                            .queryParam("daily", "temperature_2m_mean")
                            .build())
                    .retrieve()
                    .bodyToMono(WeatherResponse.class)
                    .block();

            if (weatherResponse == null || weatherResponse.getDaily() == null) {
                return "No weather data available";
            }

            List<String> dates = weatherResponse.getDaily().getTime();
            List<Double> temperatures = weatherResponse.getDaily().getTemperature2mMean();
            
            // Create a list of DailyForecast objects mapping each date to its temperature
            List<DailyForecast> forecasts = IntStream.range(0, Math.min(dates.size(), temperatures.size()))
                    .mapToObj(i -> new DailyForecast(dates.get(i), temperatures.get(i)))
                    .toList();
            
            // Return as a formatted string showing each date-temperature pair
            return forecasts.stream()
                    .map(DailyForecast::toString)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("No forecast data available");
                    
        } catch (Exception e) {
            return "Error fetching weather data for " + name + ", " + countrycode + ": " + e.getMessage();
        }
    }

}
