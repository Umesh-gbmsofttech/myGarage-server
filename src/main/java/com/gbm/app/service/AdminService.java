package com.gbm.app.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gbm.app.dto.AdminSettingsRequest;
import com.gbm.app.dto.BannerResponse;
import com.gbm.app.dto.MechanicCardResponse;
import com.gbm.app.entity.ApprovalStatus;
import com.gbm.app.entity.AdminSettings;
import com.gbm.app.entity.Banner;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.entity.MechanicRegistrationSource;
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
    private final ImageService imageService;
    private final ImageUrlService imageUrlService;
    private final MechanicService mechanicService;

    @Transactional(readOnly = true)
    public List<BannerResponse> listBanners() {
        return bannerRepository.findAll().stream().map(this::toBannerResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banner not found"));
        imageService.deleteIfPresent(banner.getImageId());
        bannerRepository.delete(banner);
    }

    @Transactional
    public BannerResponse uploadBanner(MultipartFile file) {
        Banner banner = new Banner();
        banner.setActive(true);
        Banner savedBanner = bannerRepository.save(banner);
        Long imageId = imageService.upload(file, "BANNER", savedBanner.getId()).getId();
        savedBanner.setImageId(imageId);
        return toBannerResponse(bannerRepository.save(savedBanner));
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

    @Transactional(readOnly = true)
    public List<MechanicCardResponse> pendingApprovals() {
        return mechanicProfileRepository.findByApprovalStatus(ApprovalStatus.PENDING).stream()
            .filter(profile -> profile.getRegistrationSource() == MechanicRegistrationSource.SELF)
            .map(profile -> mechanicService.withDistance(profile, null, null))
            .collect(Collectors.toList());
    }

    @Transactional
    public MechanicCardResponse updateApproval(Long mechanicUserId, ApprovalStatus status) {
        MechanicProfile profile = mechanicProfileRepository.findByUserId(mechanicUserId)
            .orElseThrow(() -> new IllegalArgumentException("Mechanic profile not found"));
        profile.setApprovalStatus(status);
        profile.setVisible(status == ApprovalStatus.APPROVED && profile.isVisible());
        return mechanicService.withDistance(mechanicProfileRepository.save(profile), null, null);
    }

    private BannerResponse toBannerResponse(Banner banner) {
        BannerResponse response = new BannerResponse();
        response.setId(banner.getId());
        response.setImageId(banner.getImageId());
        response.setImageUrl(imageUrlService.toImageUrl(banner.getImageId()));
        response.setActive(banner.isActive());
        return response;
    }
}
