package com.ubuntux.tchinda.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private SerpApi serpapi;
    private Email email;
    private List<String> queries;
    private List<String> eventKeywords;
    private Map<String, Integer> locationWeights;
    private int maxResultsPerQuery;

    @Data
    public static class SerpApi {
        private String apiKey;
        private String baseUrl;
    }

    @Data
    public static class Email {
        private String recipient;
        private String sender;
    }
}
