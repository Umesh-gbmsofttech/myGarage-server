package com.gbm.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gbm.app.dto.UpdateProfileRequest;
import com.gbm.app.dto.UserProfileResponse;
import com.gbm.app.dto.ProfileDocumentType;
import com.gbm.app.entity.AdminSettings;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.entity.MechanicRegistrationSource;
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
    private final ImageService imageService;
    private final ImageUrlService imageUrlService;
    private final MechanicService mechanicService;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setFirstName(user.getFirstName());
        response.setSurname(user.getSurname());
        response.setEmail(user.getEmail());
        response.setMobile(user.getMobile());
        response.setRole(user.getRole());

        if (user.getRole() == Role.MECHANIC || user.getRole() == Role.GARAGE_OWNER) {
            MechanicProfile profile = mechanicProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                response.setExperience(profile.getExperience());
                response.setSpeciality(profile.getSpeciality());
                response.setCity(profile.getCity());
                response.setRating(profile.getRating());
                response.setRatingCount(profile.getRatingCount());
                response.setVisible(profile.isVisible());
                response.setProfileImageUrl(imageUrlService.toImageUrl(profile.getProfileImageId()));
                response.setExpertise(profile.getExpertise());
                response.setAbout(profile.getAbout());
                response.setCertificate(profile.getCertificate());
                response.setApprovalStatus(profile.getApprovalStatus());
                response.setRegistrationSource(profile.getRegistrationSource());
                response.setGarageOwnerUserId(profile.getGarageOwner() != null ? profile.getGarageOwner().getId() : null);
                response.setAvailable(profile.isAvailable());
                response.setCertificationDocumentUrl(imageUrlService.toImageUrl(profile.getCertificationDocumentImageId()));
                response.setShopActDocumentUrl(imageUrlService.toImageUrl(profile.getShopActDocumentImageId()));
            }
            response.setGarageOwnerEligible(user.getRole() == Role.MECHANIC);
        } else if (user.getRole() == Role.VEHICLE_OWNER) {
            response.setGarageOwnerEligible(false);
        }
        
        if (user.getRole() == Role.VEHICLE_OWNER) {
            VehicleOwnerProfile profile = vehicleOwnerProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                response.setCity(profile.getCity());
                response.setAddressLine(profile.getAddressLine());
                response.setAvatarUrl(imageUrlService.toImageUrl(profile.getAvatarImageId()));
            }
        }

        if (user.getRole() == Role.ADMIN) {
            AdminSettings settings = adminSettingsRepository.findAll().stream().findFirst().orElseGet(() -> {
                AdminSettings created = new AdminSettings();
                return adminSettingsRepository.save(created);
            });
            response.setShowAllMechanics(settings.isShowAllMechanics());
        }

        if (user.getRole() == Role.GARAGE_OWNER) {
            response.setMyMechanics(mechanicService.garageWorkers(user));
        }

        return response;
    }

    @Transactional
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

        if (user.getRole() == Role.MECHANIC || user.getRole() == Role.GARAGE_OWNER) {
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
            if (request.getExpertise() != null) {
                profile.setExpertise(request.getExpertise());
            }
            if (request.getAbout() != null) {
                profile.setAbout(request.getAbout());
            }
            if (request.getCertificate() != null && profile.getRegistrationSource() == MechanicRegistrationSource.SELF) {
                profile.setCertificate(request.getCertificate());
            }
            if (request.getAvailable() != null && profile.getGarageOwner() == null) {
                profile.setAvailable(request.getAvailable());
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
            vehicleOwnerProfileRepository.save(profile);
        }

        return getProfile(user);
    }

    @Transactional
    public UserProfileResponse uploadProfileImage(User user, org.springframework.web.multipart.MultipartFile file) {
        if (user.getRole() == Role.MECHANIC || user.getRole() == Role.GARAGE_OWNER) {
            MechanicProfile profile = mechanicProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Mechanic profile missing"));
            Long imageId = imageService.upsertImageId(profile.getProfileImageId(), file, "PROFILE", user.getId());
            profile.setProfileImageId(imageId);
            mechanicProfileRepository.save(profile);
        } else if (user.getRole() == Role.VEHICLE_OWNER) {
            VehicleOwnerProfile profile = vehicleOwnerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Owner profile missing"));
            Long imageId = imageService.upsertImageId(profile.getAvatarImageId(), file, "PROFILE", user.getId());
            profile.setAvatarImageId(imageId);
            vehicleOwnerProfileRepository.save(profile);
        }
        return getProfile(user);
    }

    @Transactional
    public UserProfileResponse uploadDocument(User user, org.springframework.web.multipart.MultipartFile file, ProfileDocumentType type) {
        if (user.getRole() != Role.MECHANIC && user.getRole() != Role.GARAGE_OWNER) {
            throw new IllegalArgumentException("Only mechanics and garage owners can upload documents");
        }
        MechanicProfile profile = mechanicProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Mechanic profile missing"));
        if (type == ProfileDocumentType.CERTIFICATION) {
            Long imageId = imageService.upsertImageId(profile.getCertificationDocumentImageId(), file, "CERTIFICATION", user.getId());
            profile.setCertificationDocumentImageId(imageId);
        } else if (type == ProfileDocumentType.SHOP_ACT) {
            Long imageId = imageService.upsertImageId(profile.getShopActDocumentImageId(), file, "SHOP_ACT", user.getId());
            profile.setShopActDocumentImageId(imageId);
        }
        mechanicProfileRepository.save(profile);
        return getProfile(user);
    }
}
