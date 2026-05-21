package io.github.ashwith.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class WeatherTool implements LLMTool {

    private static final Map<String, Map<String, String>> MOCK_DATA = Map.of(
            "london",   Map.of("temp", "15°C", "condition", "Cloudy",        "humidity", "78%", "wind", "12 km/h NW"),
            "paris",    Map.of("temp", "18°C", "condition", "Partly Cloudy", "humidity", "65%", "wind", "8 km/h SW"),
            "new york", Map.of("temp", "22°C", "condition", "Sunny",         "humidity", "55%", "wind", "15 km/h NE"),
            "tokyo",    Map.of("temp", "26°C", "condition", "Clear",         "humidity", "70%", "wind", "5 km/h SE"),
            "mumbai",   Map.of("temp", "32°C", "condition", "Humid",         "humidity", "85%", "wind", "20 km/h SW"),
            "berlin",   Map.of("temp", "11°C", "condition", "Rainy",         "humidity", "88%", "wind", "18 km/h NW"),
            "sydney",   Map.of("temp", "21°C", "condition", "Clear",         "humidity", "60%", "wind", "14 km/h SE")
    );

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() { return "get_weather"; }

    @Override
    public String description() { return "Returns current weather conditions for a given city including temperature, condition, humidity and wind."; }

    @Override
    public Map<String, Object> parameters() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "city", Map.of("type", "string", "description", "The city to get weather for, e.g. London")
                ),
                "required", List.of("city")
        );
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            String city = mapper.readTree(argumentsJson).path("city").asText("").toLowerCase().trim();
            Map<String, String> data = MOCK_DATA.getOrDefault(city,
                    Map.of("temp", "20°C", "condition", "Clear", "humidity", "60%", "wind", "10 km/h"));
            return String.format(
                    "{\"city\":\"%s\",\"temperature\":\"%s\",\"condition\":\"%s\",\"humidity\":\"%s\",\"wind\":\"%s\"}",
                    city, data.get("temp"), data.get("condition"), data.get("humidity"), data.get("wind")
            );
        } catch (Exception e) {
            return "{\"error\":\"Could not retrieve weather for the requested city\"}";
        }
    }
}
