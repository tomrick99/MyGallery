package com.tomrick.mygallery.photo.api;

import com.tomrick.mygallery.photo.api.dto.ArchiveYearResponse;
import com.tomrick.mygallery.photo.api.dto.PhotoDetailResponse;
import com.tomrick.mygallery.photo.api.dto.PhotoSummaryResponse;
import com.tomrick.mygallery.photo.application.PhotoQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PhotoController {

    private final PhotoQueryService photoQueryService;

    public PhotoController(PhotoQueryService photoQueryService) {
        this.photoQueryService = photoQueryService;
    }

    @GetMapping("/photos")
    public List<PhotoSummaryResponse> getPhotos() {
        return photoQueryService.findAllPublic();
    }

    @GetMapping("/photos/featured")
    public List<PhotoSummaryResponse> getFeaturedPhotos() {
        return photoQueryService.findFeaturedPublic();
    }

    @GetMapping("/photos/{id}")
    public PhotoDetailResponse getPhoto(@PathVariable UUID id) {
        return photoQueryService.findPublicById(id);
    }

    @GetMapping("/archive")
    public List<ArchiveYearResponse> getArchive() {
        return photoQueryService.getPublicArchive();
    }
}
