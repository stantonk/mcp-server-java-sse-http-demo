package com.github.stantonk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class WeatherGetter {
    private static final Logger log = LoggerFactory.getLogger(WeatherGetter.class);
    private static final String NWS_API_BASE = "https://api.weather.gov";
    private static final String USER_AGENT = "weather-app/1.0";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    private static final String FORECAST_TEMPLATE = """
%s:
Temperature: %s°%s
Wind: %s %s
Forecast: %s
""";

    public WeatherGetter() {}

    public String getForecast(double lat, double lon) throws IOException, InterruptedException {
        String url = String.format("%s/points/%s,%s", NWS_API_BASE, lat, lon);
        var json = makeRequest(url);
        if (json.isEmpty()) throw new RuntimeException("Unable to retrieve weather data");

        String forecastUrl = json.get("properties").get("forecast").asText();
        json = makeRequest(forecastUrl);
        if (json.isEmpty()) throw new RuntimeException("Unable to retrieve weather data");
        var periods = json.get("properties").get("periods");
        if (!periods.isArray()) throw new RuntimeException("Unable to retrieve weather data");
        List<String> forecasts = new ArrayList<>();
        for (int i=0; i < 5; ++i) {
            forecasts.add(String.format(
                    FORECAST_TEMPLATE,
                    periods.get(i).get("name").asText(),
                    periods.get(i).get("temperature").asText(),
                    periods.get(i).get("temperatureUnit").asText(),
                    periods.get(i).get("windSpeed").asText(),
                    periods.get(i).get("windDirection").asText(),
                    periods.get(i).get("detailedForecast").asText()
            ));
        }
        log.info(forecastUrl);
        var fullForecast = String.join("\n---\n", forecasts);
        log.info("{}, {}, {}", lat, lon, fullForecast);
        return fullForecast;
    }

    private JsonNode makeRequest(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .headers("User-Agent", USER_AGENT,
                        "Accept", "application/geo+json")
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readValue(response.body(), JsonNode.class);
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        WeatherGetter weatherGetter = new WeatherGetter();
        double lat = 40.7128;
        double lon = -74.0060;
        System.out.println(weatherGetter.getForecast(lat, lon));
    }
}
