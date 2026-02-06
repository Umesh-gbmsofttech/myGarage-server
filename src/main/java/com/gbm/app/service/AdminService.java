package com.gbm.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gbm.app.dto.AdminSettingsRequest;
import com.gbm.app.dto.BannerRequest;
import com.gbm.app.entity.AdminSettings;
import com.gbm.app.entity.Banner;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.repository.AdminSettingsRepository;
import com.gbm.app.repository.BannerRepository;
import com.gbm.app.repository.MechanicProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final BannerRepository bannerRepository;
    private final AdminSettingsRepository adminSettingsRepository;
    private final MechanicProfileRepository mechanicProfileRepository;

    public List<Banner> listBanners() {
        return bannerRepository.findAll();
    }

    public Banner createBanner(BannerRequest request) {
        Banner banner = new Banner();
        banner.setImageUrl(request.getImageUrl());
        if (request.getActive() != null) {
            banner.setActive(request.getActive());
        }
        return bannerRepository.save(banner);
    }

    public Banner updateBanner(Long id, BannerRequest request) {
        Banner banner = bannerRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Banner not found"));
        if (request.getImageUrl() != null) {
            banner.setImageUrl(request.getImageUrl());
        }
        if (request.getActive() != null) {
            banner.setActive(request.getActive());
        }
        return bannerRepository.save(banner);
    }

    public void deleteBanner(Long id) {
        bannerRepository.deleteById(id);
    }

    public AdminSettings getSettings() {
        return adminSettingsRepository.findAll().stream().findFirst().orElseGet(() -> {
            AdminSettings settings = new AdminSettings();
            return adminSettingsRepository.save(settings);
        });
    }

    public AdminSettings updateSettings(AdminSettingsRequest request) {
        AdminSettings settings = getSettings();
        if (request.getShowAllMechanics() != null) {
            settings.setShowAllMechanics(request.getShowAllMechanics());
        }
        return adminSettingsRepository.save(settings);
    }

    public MechanicProfile updateMechanicVisibility(Long mechanicUserId, boolean visible) {
        MechanicProfile profile = mechanicProfileRepository.findByUserId(mechanicUserId)
            .orElseThrow(() -> new IllegalArgumentException("Mechanic profile not found"));
        profile.setVisible(visible);
        return mechanicProfileRepository.save(profile);
    }
}
