package com.tomrick.mygallery.photo.admin.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AdminUploadProperties.class)
public class AdminUploadConfiguration {
}
