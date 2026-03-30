package com.gbm.app.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gbm.app.dto.GarageOwnerRegistrationRequest;
import com.gbm.app.dto.GarageWorkerRequest;
import com.gbm.app.dto.MechanicCardResponse;
import com.gbm.app.dto.UserProfileResponse;
import com.gbm.app.entity.ApprovalStatus;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.entity.MechanicRegistrationSource;
import com.gbm.app.entity.Role;
import com.gbm.app.entity.User;
import com.gbm.app.repository.BookingRepository;
import com.gbm.app.repository.MechanicProfileRepository;
import com.gbm.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GarageOwnerService {

    private final UserRepository userRepository;
    private final MechanicProfileRepository mechanicProfileRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final MechanicService mechanicService;

    @Transactional
    public UserProfileResponse registerAsGarageOwner(User user, GarageOwnerRegistrationRequest request) {
        if (user.getRole() != Role.MECHANIC) {
            throw new IllegalArgumentException("Only self-registered mechanics can register as garage owners");
        }
        MechanicProfile profile = mechanicProfileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Mechanic profile missing"));
        user.setRole(Role.GARAGE_OWNER);
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getSpeciality() != null) profile.setSpeciality(request.getSpeciality());
        if (request.getExpertise() != null) profile.setExpertise(request.getExpertise());
        if (request.getAbout() != null) profile.setAbout(request.getAbout());
        profile.setApprovalStatus(ApprovalStatus.PENDING);
        userRepository.save(user);
        mechanicProfileRepository.save(profile);
        return profileService.getProfile(user);
    }

    @Transactional(readOnly = true)
    public List<MechanicCardResponse> listMyMechanics(User garageOwner) {
        ensureGarageOwner(garageOwner);
        return mechanicService.garageWorkers(garageOwner);
    }

    @Transactional
    public MechanicCardResponse addWorker(User garageOwner, GarageWorkerRequest request) {
        ensureGarageOwner(garageOwner);
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        User worker = new User();
        worker.setEmail(request.getEmail().trim().toLowerCase());
        worker.setUsername(generateUsername(worker.getEmail()));
        worker.setFirstName(request.getFirstName());
        worker.setSurname(request.getSurname());
        worker.setMobile(request.getMobile());
        worker.setRole(Role.MECHANIC);
        worker.setPasswordHash(passwordEncoder.encode(request.getPassword() == null || request.getPassword().isBlank() ? "GarageWorker@123" : request.getPassword()));

        MechanicProfile profile = new MechanicProfile();
        profile.setUser(worker);
        profile.setGarageOwner(garageOwner);
        profile.setRegistrationSource(MechanicRegistrationSource.GARAGE_OWNER);
        profile.setApprovalStatus(ApprovalStatus.APPROVED);
        profile.setVisible(true);
        profile.setAvailable(true);
        profile.setExperience(request.getExperience());
        profile.setSpeciality(request.getSpeciality());
        profile.setExpertise(request.getExpertise() != null ? request.getExpertise() : request.getSpeciality());
        profile.setCity(request.getCity());
        profile.setAbout(request.getAbout());
        mechanicProfileRepository.save(profile);
        return mechanicService.withDistance(profile, null, null);
    }

    @Transactional
    public MechanicCardResponse updateWorker(User garageOwner, Long workerUserId, GarageWorkerRequest request) {
        ensureGarageOwner(garageOwner);
        MechanicProfile profile = mechanicProfileRepository.findByUserId(workerUserId)
            .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        if (profile.getGarageOwner() == null || !profile.getGarageOwner().getId().equals(garageOwner.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        User worker = profile.getUser();
        if (request.getFirstName() != null) worker.setFirstName(request.getFirstName());
        if (request.getSurname() != null) worker.setSurname(request.getSurname());
        if (request.getMobile() != null) worker.setMobile(request.getMobile());
        if (request.getExperience() != null) profile.setExperience(request.getExperience());
        if (request.getSpeciality() != null) profile.setSpeciality(request.getSpeciality());
        if (request.getExpertise() != null) profile.setExpertise(request.getExpertise());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getAbout() != null) profile.setAbout(request.getAbout());
        userRepository.save(worker);
        mechanicProfileRepository.save(profile);
        return mechanicService.withDistance(profile, null, null);
    }

    @Transactional
    public void deleteWorker(User garageOwner, Long workerUserId) {
        ensureGarageOwner(garageOwner);
        MechanicProfile profile = mechanicProfileRepository.findByUserId(workerUserId)
            .orElseThrow(() -> new IllegalArgumentException("Worker not found"));
        if (profile.getGarageOwner() == null || !profile.getGarageOwner().getId().equals(garageOwner.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        bookingRepository.findByAssignedWorkerId(workerUserId).forEach(booking -> {
            booking.setAssignedWorker(null);
            bookingRepository.save(booking);
        });
        mechanicProfileRepository.delete(profile);
        userRepository.delete(profile.getUser());
    }

    private void ensureGarageOwner(User user) {
        if (user.getRole() != Role.GARAGE_OWNER) {
            throw new IllegalArgumentException("Garage owner access required");
        }
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0];
        String username = base;
        int counter = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = base + counter;
            counter++;
        }
        return username;
    }
}
