package com.tomrick.mygallery.photo.admin.api;

import com.tomrick.mygallery.photo.admin.api.dto.UploadSignatureRequest;
import com.tomrick.mygallery.photo.admin.api.dto.UploadSignatureResponse;
import com.tomrick.mygallery.photo.admin.application.AdminUploadSignatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/uploads")
public class AdminUploadController {

    private final AdminUploadSignatureService signatureService;

    public AdminUploadController(AdminUploadSignatureService signatureService) {
        this.signatureService = signatureService;
    }

    @PostMapping("/signature")
    public ResponseEntity<UploadSignatureResponse> createSignature(
            @Valid @RequestBody UploadSignatureRequest request,
            HttpServletRequest servletRequest
    ) {
        HttpSession session = servletRequest.getSession(false);
        String sessionId = session == null ? "" : session.getId();
        UploadSignatureResponse response = signatureService.issue(
                request,
                sessionId,
                servletRequest.getRemoteAddr()
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(response);
    }
}
