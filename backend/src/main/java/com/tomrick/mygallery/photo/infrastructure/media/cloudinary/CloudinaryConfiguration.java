package com.tomrick.mygallery.photo.infrastructure.media.cloudinary;

import com.cloudinary.Cloudinary;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Map;

@Configuration(proxyBeanMethods = false)
@Profile("cloudinary")
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfiguration {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        return new Cloudinary(Map.of(
                "cloud_name", required(properties.cloudName(), "CLOUDINARY_CLOUD_NAME"),
                "api_key", required(properties.apiKey(), "CLOUDINARY_API_KEY"),
                "api_secret", required(properties.apiSecret(), "CLOUDINARY_API_SECRET"),
                "secure", true
        ));
    }

    private static String required(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " must not be blank");
        }
        return value;
    }
}

@ConfigurationProperties(prefix = "mygallery.cloudinary")
record CloudinaryProperties(
        String cloudName,
        String apiKey,
        String apiSecret
) {
}
