package com.tomrick.mygallery.photo.infrastructure.persistence;

import com.tomrick.mygallery.photo.domain.PhotoVisibility;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "photos")
class PhotoEntity {

    @Id
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @PastOrPresent
    @Column(name = "taken_at", nullable = false)
    private LocalDate takenAt;

    @Size(max = 200)
    @Column(length = 200)
    private String location;

    @NotBlank
    @Size(max = 255)
    @Column(name = "cloudinary_public_id", nullable = false, unique = true, length = 255)
    private String cloudinaryPublicId;

    @Positive
    @Column(nullable = false)
    private int width;

    @Positive
    @Column(nullable = false)
    private int height;

    @Column(nullable = false)
    private boolean featured;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PhotoVisibility visibility;

    @Size(max = 150)
    @Column(length = 150)
    private String camera;

    @Size(max = 200)
    @Column(length = 200)
    private String lens;

    @Positive
    @Column(name = "focal_length", precision = 8, scale = 2)
    private BigDecimal focalLength;

    @Positive
    @Column(precision = 5, scale = 2)
    private BigDecimal aperture;

    @Positive
    @Column(name = "shutter_speed", precision = 16, scale = 9)
    private BigDecimal shutterSpeed;

    @Positive
    private Integer iso;

    @Size(max = 5000)
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PhotoEntity() {
    }

    PhotoEntity(
            UUID id,
            String title,
            LocalDate takenAt,
            String location,
            String cloudinaryPublicId,
            int width,
            int height,
            boolean featured,
            PhotoVisibility visibility,
            String camera,
            String lens,
            BigDecimal focalLength,
            BigDecimal aperture,
            BigDecimal shutterSpeed,
            Integer iso,
            String description,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.title = title;
        this.takenAt = takenAt;
        this.location = location;
        this.cloudinaryPublicId = cloudinaryPublicId;
        this.width = width;
        this.height = height;
        this.featured = featured;
        this.visibility = visibility;
        this.camera = camera;
        this.lens = lens;
        this.focalLength = focalLength;
        this.aperture = aperture;
        this.shutterSpeed = shutterSpeed;
        this.iso = iso;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    void prepareForInsert() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void prepareForUpdate() {
        updatedAt = Instant.now();
    }

    void updateMetadata(
            String title,
            LocalDate takenAt,
            String location,
            boolean featured,
            PhotoVisibility visibility,
            String camera,
            String lens,
            BigDecimal focalLength,
            BigDecimal aperture,
            BigDecimal shutterSpeed,
            Integer iso,
            String description
    ) {
        this.title = title;
        this.takenAt = takenAt;
        this.location = location;
        this.featured = featured;
        this.visibility = visibility;
        this.camera = camera;
        this.lens = lens;
        this.focalLength = focalLength;
        this.aperture = aperture;
        this.shutterSpeed = shutterSpeed;
        this.iso = iso;
        this.description = description;
    }

    UUID getId() {
        return id;
    }

    String getTitle() {
        return title;
    }

    LocalDate getTakenAt() {
        return takenAt;
    }

    String getLocation() {
        return location;
    }

    String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }

    int getWidth() {
        return width;
    }

    int getHeight() {
        return height;
    }

    boolean isFeatured() {
        return featured;
    }

    PhotoVisibility getVisibility() {
        return visibility;
    }

    String getCamera() {
        return camera;
    }

    String getLens() {
        return lens;
    }

    BigDecimal getFocalLength() {
        return focalLength;
    }

    BigDecimal getAperture() {
        return aperture;
    }

    BigDecimal getShutterSpeed() {
        return shutterSpeed;
    }

    Integer getIso() {
        return iso;
    }

    String getDescription() {
        return description;
    }
}
