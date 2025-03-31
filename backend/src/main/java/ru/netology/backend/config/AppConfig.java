package ru.netology.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class AppConfig {
    @Bean
    @ConfigurationProperties(prefix = "app")
    public AppProperties appProperties() {
        return new AppProperties();
    }

    @PostConstruct
    public void init() {
        try {
            Path storageLocation = Paths.get(appProperties().getStorage().getLocation());
            if (!Files.exists(storageLocation)) {
                Files.createDirectories(storageLocation);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    @Getter
    @Setter
    public static class AppProperties {
        private Security security;
        private Storage storage;

        @Getter
        @Setter
        public static class Security {
            private String tokenSecret;
            private long tokenExpirationMs;
        }

        @Getter
        @Setter
        public static class Storage {
            private String location;
        }
    }
}
