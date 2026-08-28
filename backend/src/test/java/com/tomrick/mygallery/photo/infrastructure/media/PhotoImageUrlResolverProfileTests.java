package com.tomrick.mygallery.photo.infrastructure.media;

import com.tomrick.mygallery.photo.infrastructure.media.cloudinary.CloudinaryConfiguration;
import com.tomrick.mygallery.photo.infrastructure.media.cloudinary.CloudinaryPhotoImageUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoImageUrlResolverProfileTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    DevelopmentPhotoImageUrlResolver.class,
                    CloudinaryConfiguration.class,
                    CloudinaryPhotoImageUrlResolver.class
            );

    @Test
    void postgresProfileSelectsOnlyTheDevelopmentResolver() {
        contextRunner
                .withPropertyValues("spring.profiles.active=postgres")
                .run(context -> {
                    assertThat(context).hasSingleBean(PhotoImageUrlResolver.class);
                    assertThat(context.getBean(PhotoImageUrlResolver.class))
                            .isInstanceOf(DevelopmentPhotoImageUrlResolver.class);
                });
    }

    @Test
    void postgresAndCloudinaryProfilesSelectOnlyTheCloudinaryResolver() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=postgres,cloudinary",
                        "mygallery.cloudinary.cloud-name=test-cloud",
                        "mygallery.cloudinary.api-key=test-api-key",
                        "mygallery.cloudinary.api-secret=test-api-secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(PhotoImageUrlResolver.class);
                    assertThat(context.getBean(PhotoImageUrlResolver.class))
                            .isInstanceOf(CloudinaryPhotoImageUrlResolver.class);
                });
    }

    @Test
    void cloudinaryProfileFailsFastWhenCredentialsAreMissing() {
        contextRunner
                .withPropertyValues("spring.profiles.active=postgres,cloudinary")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("CLOUDINARY_CLOUD_NAME must not be blank");
                });
    }
}
