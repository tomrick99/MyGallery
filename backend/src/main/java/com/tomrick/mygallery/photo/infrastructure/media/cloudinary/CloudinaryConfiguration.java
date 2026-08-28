package com.tomrick.mygallery.photo.infrastructure.media.cloudinary;

import com.cloudinary.Cloudinary;
import com.tomrick.mygallery.photo.admin.application.AdminUploadProperties;
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
    public Cloudinary cloudinary(
            CloudinaryProperties properties,
            AdminUploadProperties uploadProperties
    ) {
        String cloudName = required(properties.cloudName(), "CLOUDINARY_CLOUD_NAME");
        String apiKey = required(properties.apiKey(), "CLOUDINARY_API_KEY");
        required(uploadProperties.uploadPreset(), "CLOUDINARY_UPLOAD_PRESET");
        if (!cloudName.equals(uploadProperties.cloudName())
                || !apiKey.equals(uploadProperties.apiKey())) {
            throw new IllegalStateException(
                    "Cloudinary upload identifiers must match the configured Cloudinary account"
            );
        }
        return new Cloudinary(Map.of(
                "cloud_name", cloudName,
                "api_key", apiKey,
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
