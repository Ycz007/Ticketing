package com.ticketing.attachment.controller;

import com.ticketing.attachment.model.AttachmentRow;
import com.ticketing.attachment.service.AttachmentService;
import com.ticketing.auth.security.SessionUser;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {
    private final AttachmentService service;
    public AttachmentController(AttachmentService service) { this.service=service; }
    @PostMapping
    public AttachmentRow upload(@AuthenticationPrincipal SessionUser user,@RequestParam MultipartFile file) {
        return service.upload(user,file);
    }
    @GetMapping("/{id}/content")
    public ResponseEntity<byte[]> content(@AuthenticationPrincipal SessionUser user,@PathVariable Long id) {
        var image=service.read(user,id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(image.mediaType()))
                .cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(image.bytes());
    }
}
