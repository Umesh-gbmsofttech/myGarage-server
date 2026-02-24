package com.gbm.app.controller;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.gbm.app.dto.ImageResponse;
import com.gbm.app.service.ImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping
    public ResponseEntity<ImageResponse> upload(@RequestParam("file") MultipartFile file,
            @RequestParam("referenceType") String referenceType,
            @RequestParam("referenceId") Long referenceId) {
        return ResponseEntity.ok(imageService.upload(file, referenceType, referenceId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return imageService.download(id);
    }

    @GetMapping("/{id}/metadata")
    public ResponseEntity<ImageResponse> metadata(@PathVariable Long id) {
        return ResponseEntity.ok(imageService.getMetadata(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImageResponse> update(@PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @RequestParam("referenceType") String referenceType,
            @RequestParam("referenceId") Long referenceId) {
        return ResponseEntity.ok(imageService.update(id, file, referenceType, referenceId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        imageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ImageResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(imageService.list(pageable));
    }
}
