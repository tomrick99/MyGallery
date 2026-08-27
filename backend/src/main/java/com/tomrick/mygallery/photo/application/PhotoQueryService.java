package com.tomrick.mygallery.photo.application;

import com.tomrick.mygallery.photo.api.dto.ArchiveMonthResponse;
import com.tomrick.mygallery.photo.api.dto.ArchiveYearResponse;
import com.tomrick.mygallery.photo.api.dto.PhotoDetailResponse;
import com.tomrick.mygallery.photo.api.dto.PhotoImageResponse;
import com.tomrick.mygallery.photo.api.dto.PhotoSummaryResponse;
import com.tomrick.mygallery.photo.domain.Photo;
import com.tomrick.mygallery.photo.domain.PhotoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class PhotoQueryService {

    private static final Comparator<Photo> NEWEST_FIRST = Comparator
            .comparing(Photo::takenAt, Comparator.reverseOrder())
            .thenComparing(Photo::id, Comparator.reverseOrder());

    private final PhotoRepository photoRepository;

    public PhotoQueryService(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    public List<PhotoSummaryResponse> findAllPublic() {
        return sortedPublicPhotos().stream()
                .map(this::toSummary)
                .toList();
    }

    public List<PhotoSummaryResponse> findFeaturedPublic() {
        return sortedPublicPhotos().stream()
                .filter(Photo::featured)
                .map(this::toSummary)
                .toList();
    }

    public PhotoDetailResponse findPublicById(UUID id) {
        Photo photo = photoRepository.findPublicById(id)
                .orElseThrow(PhotoNotFoundException::new);

        return toDetail(photo);
    }

    public List<ArchiveYearResponse> getPublicArchive() {
        Map<Integer, Map<Integer, List<PhotoSummaryResponse>>> grouped = new HashMap<>();

        for (PhotoSummaryResponse photo : findAllPublic()) {
            grouped
                    .computeIfAbsent(photo.year(), ignored -> new HashMap<>())
                    .computeIfAbsent(photo.month(), ignored -> new ArrayList<>())
                    .add(photo);
        }

        return grouped.entrySet().stream()
                .sorted(Map.Entry.<Integer, Map<Integer, List<PhotoSummaryResponse>>>comparingByKey().reversed())
                .map(yearEntry -> toArchiveYear(yearEntry.getKey(), yearEntry.getValue()))
                .toList();
    }

    private List<Photo> sortedPublicPhotos() {
        return photoRepository.findAllPublic().stream()
                .sorted(NEWEST_FIRST)
                .toList();
    }

    private ArchiveYearResponse toArchiveYear(
            int year,
            Map<Integer, List<PhotoSummaryResponse>> photosByMonth
    ) {
        List<ArchiveMonthResponse> months = photosByMonth.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<PhotoSummaryResponse>>comparingByKey().reversed())
                .map(monthEntry -> new ArchiveMonthResponse(
                        monthEntry.getKey(),
                        monthLabel(monthEntry.getKey()),
                        monthEntry.getValue().size(),
                        List.copyOf(monthEntry.getValue())
                ))
                .toList();

        int photoCount = months.stream()
                .mapToInt(ArchiveMonthResponse::photoCount)
                .sum();

        return new ArchiveYearResponse(year, photoCount, months);
    }

    private PhotoSummaryResponse toSummary(Photo photo) {
        return new PhotoSummaryResponse(
                photo.id(),
                photo.title(),
                photo.takenAt(),
                photo.takenAt().getYear(),
                photo.takenAt().getMonthValue(),
                photo.location(),
                orientation(photo),
                (double) photo.width() / photo.height(),
                photo.featured(),
                new PhotoImageResponse(
                        photo.thumbnailUrl(),
                        photo.cardUrl(),
                        photo.displayUrl()
                )
        );
    }

    private PhotoDetailResponse toDetail(Photo photo) {
        PhotoSummaryResponse summary = toSummary(photo);

        return new PhotoDetailResponse(
                summary.id(),
                summary.title(),
                summary.takenAt(),
                summary.year(),
                summary.month(),
                summary.location(),
                summary.orientation(),
                summary.aspectRatio(),
                summary.featured(),
                summary.image(),
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

    private static String monthLabel(int month) {
        return Month.of(month)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toUpperCase(Locale.ENGLISH);
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
