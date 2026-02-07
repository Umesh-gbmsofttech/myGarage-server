package com.gbm.app.service;

import org.springframework.stereotype.Service;

import com.gbm.app.dto.UpdateProfileRequest;
import com.gbm.app.dto.UserProfileResponse;
import com.gbm.app.entity.AdminSettings;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.entity.Role;
import com.gbm.app.entity.User;
import com.gbm.app.entity.VehicleOwnerProfile;
import com.gbm.app.repository.AdminSettingsRepository;
import com.gbm.app.repository.MechanicProfileRepository;
import com.gbm.app.repository.UserRepository;
import com.gbm.app.repository.VehicleOwnerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final MechanicProfileRepository mechanicProfileRepository;
    private final VehicleOwnerProfileRepository vehicleOwnerProfileRepository;
    private final AdminSettingsRepository adminSettingsRepository;
    private final ProfileImageStorageService profileImageStorageService;

    public UserProfileResponse getProfile(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstName(user.getFirstName());
        response.setSurname(user.getSurname());
        response.setEmail(user.getEmail());
        response.setMobile(user.getMobile());
        response.setRole(user.getRole());

        if (user.getRole() == Role.MECHANIC) {
            MechanicProfile profile = mechanicProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                response.setExperience(profile.getExperience());
                response.setSpeciality(profile.getSpeciality());
                response.setCity(profile.getCity());
                response.setShopActive(profile.isShopActive());
                response.setRating(profile.getRating());
                response.setRatingCount(profile.getRatingCount());
                response.setVisible(profile.isVisible());
                response.setProfileImageUrl(profile.getProfileImageUrl());
                response.setExpertise(profile.getExpertise());
                response.setAbout(profile.getAbout());
            }
        }

        if (user.getRole() == Role.VEHICLE_OWNER) {
            VehicleOwnerProfile profile = vehicleOwnerProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                response.setCity(profile.getCity());
                response.setAddressLine(profile.getAddressLine());
                response.setAvatarUrl(profile.getAvatarUrl());
            }
        }

        if (user.getRole() == Role.ADMIN) {
            AdminSettings settings = adminSettingsRepository.findAll().stream().findFirst().orElseGet(() -> {
                AdminSettings created = new AdminSettings();
                return adminSettingsRepository.save(created);
            });
            response.setShowAllMechanics(settings.isShowAllMechanics());
        }

        return response;
    }

    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getSurname() != null) {
            user.setSurname(request.getSurname());
        }
        if (request.getMobile() != null) {
            user.setMobile(request.getMobile());
        }
        userRepository.save(user);

        if (user.getRole() == Role.MECHANIC) {
            MechanicProfile profile = mechanicProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mechanic profile missing"));
            if (request.getExperience() != null) {
                profile.setExperience(request.getExperience());
            }
            if (request.getSpeciality() != null) {
                profile.setSpeciality(request.getSpeciality());
            }
            if (request.getCity() != null) {
                profile.setCity(request.getCity());
            }
            if (request.getShopActive() != null) {
                profile.setShopActive(request.getShopActive());
            }
            if (request.getProfileImageUrl() != null) {
                profile.setProfileImageUrl(request.getProfileImageUrl());
            }
            if (request.getExpertise() != null) {
                profile.setExpertise(request.getExpertise());
            }
            if (request.getAbout() != null) {
                profile.setAbout(request.getAbout());
            }
            mechanicProfileRepository.save(profile);
        }

        if (user.getRole() == Role.VEHICLE_OWNER) {
            VehicleOwnerProfile profile = vehicleOwnerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Owner profile missing"));
            if (request.getCity() != null) {
                profile.setCity(request.getCity());
            }
            if (request.getAddressLine() != null) {
                profile.setAddressLine(request.getAddressLine());
            }
            if (request.getAvatarUrl() != null) {
                profile.setAvatarUrl(request.getAvatarUrl());
            }
            vehicleOwnerProfileRepository.save(profile);
        }

        return getProfile(user);
    }

    public UserProfileResponse uploadProfileImage(User user, org.springframework.web.multipart.MultipartFile file) {
        String url = profileImageStorageService.store(file);
        if (user.getRole() == Role.MECHANIC) {
            MechanicProfile profile = mechanicProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mechanic profile missing"));
            profile.setProfileImageUrl(url);
            mechanicProfileRepository.save(profile);
        } else if (user.getRole() == Role.VEHICLE_OWNER) {
            VehicleOwnerProfile profile = vehicleOwnerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Owner profile missing"));
            profile.setAvatarUrl(url);
            vehicleOwnerProfileRepository.save(profile);
        }
        return getProfile(user);
    }
}
