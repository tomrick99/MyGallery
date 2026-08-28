package com.tomrick.mygallery.photo.infrastructure.media;

import com.tomrick.mygallery.photo.admin.application.AdminUploadConfiguration;
import com.tomrick.mygallery.photo.infrastructure.media.cloudinary.CloudinaryConfiguration;
import com.tomrick.mygallery.photo.infrastructure.media.cloudinary.CloudinaryPhotoImageUrlResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoImageUrlResolverProfileTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(
                    AdminUploadConfiguration.class,
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
                        "mygallery.cloudinary.api-secret=test-api-secret",
                        "mygallery.cloudinary.upload.cloud-name=test-cloud",
                        "mygallery.cloudinary.upload.api-key=test-api-key",
                        "mygallery.cloudinary.upload.upload-preset=test-signed-preset"
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

    @Test
    void cloudinaryProfileFailsFastWhenUploadPresetIsMissing() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=postgres,cloudinary",
                        "mygallery.cloudinary.cloud-name=test-cloud",
                        "mygallery.cloudinary.api-key=test-api-key",
                        "mygallery.cloudinary.api-secret=test-api-secret",
                        "mygallery.cloudinary.upload.cloud-name=test-cloud",
                        "mygallery.cloudinary.upload.api-key=test-api-key"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseMessage("CLOUDINARY_UPLOAD_PRESET must not be blank");
                });
    }
}
