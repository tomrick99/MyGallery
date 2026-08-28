package com.tomrick.mygallery.photo.admin.api;

import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoCreateRequest;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoPageResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoUpdateRequest;
import com.tomrick.mygallery.photo.admin.application.AdminPhotoCreationService;
import com.tomrick.mygallery.photo.admin.application.AdminPhotoDeletionService;
import com.tomrick.mygallery.photo.admin.application.AdminPhotoService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/photos")
public class AdminPhotoController {

    private final AdminPhotoService adminPhotoService;
    private final AdminPhotoCreationService creationService;
    private final AdminPhotoDeletionService deletionService;

    public AdminPhotoController(
            AdminPhotoService adminPhotoService,
            AdminPhotoCreationService creationService,
            AdminPhotoDeletionService deletionService
    ) {
        this.adminPhotoService = adminPhotoService;
        this.creationService = creationService;
        this.deletionService = deletionService;
    }

    @GetMapping
    public ResponseEntity<AdminPhotoPageResponse> getPhotos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size
    ) {
        return noStore(adminPhotoService.findPage(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminPhotoResponse> getPhoto(@PathVariable UUID id) {
        return noStore(adminPhotoService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AdminPhotoResponse> createPhoto(
            @Valid @RequestBody AdminPhotoCreateRequest request
    ) {
        AdminPhotoResponse response = creationService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/photos/" + response.id()))
                .cacheControl(CacheControl.noStore())
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminPhotoResponse> updatePhoto(
            @PathVariable UUID id,
            @Valid @RequestBody AdminPhotoUpdateRequest request
    ) {
        return noStore(adminPhotoService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable UUID id) {
        deletionService.delete(id);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
