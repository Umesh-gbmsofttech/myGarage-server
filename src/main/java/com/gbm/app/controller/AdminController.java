package com.gbm.app.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.gbm.app.dto.AdminSettingsRequest;
import com.gbm.app.dto.BannerRequest;
import com.gbm.app.entity.AdminSettings;
import com.gbm.app.entity.Banner;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.service.AdminService;

import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/settings")
    public ResponseEntity<AdminSettings> settings() {
        return ResponseEntity.ok(adminService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<AdminSettings> updateSettings(@RequestBody AdminSettingsRequest request) {
        return ResponseEntity.ok(adminService.updateSettings(request));
    }

    @GetMapping("/banners")
    public ResponseEntity<List<Banner>> listBanners() {
        return ResponseEntity.ok(adminService.listBanners());
    }

    @PostMapping("/banners")
    public ResponseEntity<Banner> createBanner(@RequestBody BannerRequest request) {
        return ResponseEntity.ok(adminService.createBanner(request));
    }

    @PostMapping("/banners/upload")
    public ResponseEntity<Banner> uploadBanner(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(adminService.uploadBanner(file));
    }

    @PutMapping("/banners/{id}")
    public ResponseEntity<Banner> updateBanner(@PathVariable Long id, @RequestBody BannerRequest request) {
        return ResponseEntity.ok(adminService.updateBanner(id, request));
    }

    @DeleteMapping("/banners/{id}")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long id) {
        adminService.deleteBanner(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/mechanics/{id}/visibility/{visible}")
    public ResponseEntity<MechanicProfile> updateVisibility(@PathVariable Long id, @PathVariable boolean visible) {
        return ResponseEntity.ok(adminService.updateMechanicVisibility(id, visible));
    }
}
