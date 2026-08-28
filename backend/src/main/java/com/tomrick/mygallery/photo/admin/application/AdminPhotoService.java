package com.tomrick.mygallery.photo.admin.application;

import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoImageResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoPageResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoUpdateRequest;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoPage;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoRepository;
import com.tomrick.mygallery.photo.admin.domain.AdminPhotoUpdate;
import com.tomrick.mygallery.photo.domain.Photo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
public class AdminPhotoService {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 24;
    public static final int MAX_SIZE = 100;

    private final AdminPhotoRepository adminPhotoRepository;

    public AdminPhotoService(AdminPhotoRepository adminPhotoRepository) {
        this.adminPhotoRepository = adminPhotoRepository;
    }

    public AdminPhotoPageResponse findPage(int page, int size) {
        validatePage(page, size);
        AdminPhotoPage result = adminPhotoRepository.findPage(page, size);
        long totalPages = result.totalElements() == 0
                ? 0
                : ((result.totalElements() - 1) / size) + 1;

        return new AdminPhotoPageResponse(
                result.items().stream().map(AdminPhotoService::toResponse).toList(),
                page,
                size,
                result.totalElements(),
                (int) Math.min(totalPages, Integer.MAX_VALUE)
        );
    }

    public AdminPhotoResponse findById(UUID id) {
        return adminPhotoRepository.findById(id)
                .map(AdminPhotoService::toResponse)
                .orElseThrow(AdminPhotoNotFoundException::new);
    }

    public AdminPhotoResponse update(UUID id, AdminPhotoUpdateRequest request) {
        AdminPhotoUpdate update = new AdminPhotoUpdate(
                request.title(),
                request.takenAt(),
                request.location(),
                request.featured(),
                request.visibility(),
                request.camera(),
                request.lens(),
                request.focalLengthMm(),
                request.aperture(),
                request.shutterSpeedSeconds(),
                request.iso(),
                request.description()
        );

        return adminPhotoRepository.update(id, update)
                .map(AdminPhotoService::toResponse)
                .orElseThrow(AdminPhotoNotFoundException::new);
    }

    private static void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_SIZE) {
            throw new InvalidAdminPhotoFilterException();
        }
    }

    private static AdminPhotoResponse toResponse(Photo photo) {
        return new AdminPhotoResponse(
                photo.id(),
                photo.title(),
                photo.takenAt(),
                photo.takenAt().getYear(),
                photo.takenAt().getMonthValue(),
                photo.location(),
                orientation(photo),
                (double) photo.width() / photo.height(),
                photo.featured(),
                photo.visibility(),
                photo.width(),
                photo.height(),
                new AdminPhotoImageResponse(
                        photo.thumbnailUrl(),
                        photo.cardUrl(),
                        photo.displayUrl()
                ),
                photo.camera(),
                photo.lens(),
                formatDecimal(photo.focalLength(), "", " mm"),
                formatDecimal(photo.aperture(), "f/", ""),
                formatShutterSpeed(photo.shutterSpeed()),
                photo.iso(),
                photo.description()
        );
    }

    private static String orientation(Photo photo) {
        if (photo.width() > photo.height()) {
            return "landscape";
        }
        if (photo.width() < photo.height()) {
            return "portrait";
        }
        return "square";
    }

    private static String formatDecimal(BigDecimal value, String prefix, String suffix) {
        if (value == null) {
            return null;
        }
        return prefix + value.stripTrailingZeros().toPlainString() + suffix;
    }

    private static String formatShutterSpeed(BigDecimal seconds) {
        if (seconds == null) {
            return null;
        }
        if (seconds.compareTo(BigDecimal.ZERO) > 0 && seconds.compareTo(BigDecimal.ONE) < 0) {
            BigDecimal denominator = BigDecimal.ONE.divide(seconds, 0, RoundingMode.HALF_UP);
            return "1/" + denominator.toPlainString() + " s";
        }
        return formatDecimal(seconds, "", " s");
    }
}
