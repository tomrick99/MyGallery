package com.tomrick.mygallery.photo.admin.api;

import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoPageResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoResponse;
import com.tomrick.mygallery.photo.admin.api.dto.AdminPhotoUpdateRequest;
import com.tomrick.mygallery.photo.admin.application.AdminPhotoService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/photos")
public class AdminPhotoController {

    private final AdminPhotoService adminPhotoService;

    public AdminPhotoController(AdminPhotoService adminPhotoService) {
        this.adminPhotoService = adminPhotoService;
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

    @PutMapping("/{id}")
    public ResponseEntity<AdminPhotoResponse> updatePhoto(
            @PathVariable UUID id,
            @Valid @RequestBody AdminPhotoUpdateRequest request
    ) {
        return noStore(adminPhotoService.update(id, request));
    }

    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(body);
    }
}
