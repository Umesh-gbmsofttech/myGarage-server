package com.gbm.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gbm.app.dto.MechanicCardResponse;
import com.gbm.app.entity.ApprovalStatus;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.entity.Role;
import com.gbm.app.entity.User;
import com.gbm.app.repository.MechanicProfileRepository;
import com.gbm.app.util.DistanceUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MechanicService {

    private final MechanicProfileRepository mechanicProfileRepository;
    private final ImageUrlService imageUrlService;
    private final Random random = new Random();

    @Transactional(readOnly = true)
    public List<MechanicCardResponse> topRated(int limit) {
        return mechanicProfileRepository.findByVisibleTrueAndApprovalStatus(ApprovalStatus.APPROVED).stream()
            .filter(MechanicProfile::isAvailable)
            .sorted(Comparator.comparing(MechanicProfile::getRating, Comparator.nullsLast(Double::compareTo)).reversed())
            .limit(limit)
            .map(this::toCard)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MechanicCardResponse> random(int limit) {
        List<MechanicProfile> profiles = mechanicProfileRepository.findByVisibleTrueAndApprovalStatus(ApprovalStatus.APPROVED).stream()
            .filter(MechanicProfile::isAvailable)
            .collect(Collectors.toList());
        return profiles.stream()
            .sorted((a, b) -> random.nextInt(3) - 1)
            .limit(limit)
            .map(this::toCard)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MechanicCardResponse> search(String query, Double lat, Double lng, double radiusKm) {
        String safeQuery = query == null ? "" : query.toLowerCase();
        return mechanicProfileRepository.findByVisibleTrueAndApprovalStatus(ApprovalStatus.APPROVED).stream()
            .filter(MechanicProfile::isAvailable)
            .filter(profile -> matches(profile, safeQuery))
            .map(profile -> withDistance(profile, lat, lng))
            .filter(card -> card.isAvailable())
            .filter(card -> card.getDistanceKm() == null || card.getDistanceKm() <= radiusKm)
            .sorted(Comparator.comparing(MechanicCardResponse::getRating, Comparator.nullsLast(Double::compareTo)).reversed())
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MechanicCardResponse> searchPaged(String query, Double lat, Double lng, double radiusKm, int page, int size) {
        String safeQuery = query == null ? "" : query.trim();
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.desc("rating"), Sort.Order.asc("id"))
        );
        Page<MechanicProfile> profiles = safeQuery.isBlank()
            ? mechanicProfileRepository.findByVisibleTrueAndApprovalStatus(ApprovalStatus.APPROVED, pageable)
            : mechanicProfileRepository.searchVisible(safeQuery.toLowerCase(), pageable);
        List<MechanicCardResponse> filtered = profiles
            .map(profile -> withDistance(profile, lat, lng))
            .stream()
            .filter(card -> card.isAvailable())
            .filter(card -> card.getDistanceKm() == null || card.getDistanceKm() <= radiusKm)
            .collect(Collectors.toList());
        return new PageImpl<>(filtered, pageable, profiles.getTotalElements());
    }

    public MechanicCardResponse withDistance(MechanicProfile profile, Double lat, Double lng) {
        MechanicCardResponse card = toCard(profile);
        if (lat != null && lng != null && profile.getLatitude() != null && profile.getLongitude() != null) {
            card.setDistanceKm(DistanceUtil.distanceKm(lat, lng, profile.getLatitude(), profile.getLongitude()));
        }
        return card;
    }

    private boolean matches(MechanicProfile profile, String query) {
        if (query.isBlank()) {
            return true;
        }
        String name = (profile.getUser().getFirstName() + " " + profile.getUser().getSurname()).toLowerCase();
        String speciality = safe(resolveAggregatedSpeciality(profile)).toLowerCase();
        String expertise = safe(resolveAggregatedExpertise(profile)).toLowerCase();
        String city = safe(profile.getCity()).toLowerCase();
        return name.contains(query) || speciality.contains(query) || expertise.contains(query) || city.contains(query);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private MechanicCardResponse toCard(MechanicProfile profile) {
        MechanicCardResponse card = new MechanicCardResponse();
        card.setMechanicId(profile.getUser().getId());
        card.setMechName(profile.getUser().getFirstName());
        card.setSurname(profile.getUser().getSurname());
        card.setSpeciality(resolveAggregatedSpeciality(profile));
        card.setExpertise(resolveAggregatedExpertise(profile));
        card.setRating(profile.getRating());
        card.setRatingCount(profile.getRatingCount());
        card.setCity(profile.getCity());
        card.setProfileImageUrl(imageUrlService.toImageUrl(profile.getProfileImageId()));
        card.setRole(profile.getUser().getRole());
        card.setApprovalStatus(profile.getApprovalStatus());
        card.setRegistrationSource(profile.getRegistrationSource());
        card.setCertificate(profile.getCertificate());
        card.setGarageOwnerUserId(profile.getGarageOwner() != null ? profile.getGarageOwner().getId() : null);
        card.setGarageOwner(profile.getUser().getRole() == Role.GARAGE_OWNER);
        card.setAvailable(profile.isAvailable());
        card.setCertificationDocumentUrl(imageUrlService.toImageUrl(profile.getCertificationDocumentImageId()));
        card.setShopActDocumentUrl(imageUrlService.toImageUrl(profile.getShopActDocumentImageId()));
        return card;
    }

    @Transactional(readOnly = true)
    public List<MechanicCardResponse> garageWorkers(User garageOwner) {
        return mechanicProfileRepository.findByGarageOwner(garageOwner).stream()
            .sorted(Comparator.comparing(p -> safe(p.getUser().getFirstName())))
            .map(this::toCard)
            .collect(Collectors.toList());
    }

    private String resolveAggregatedSpeciality(MechanicProfile profile) {
        String base = safe(profile.getSpeciality());
        if (profile.getUser().getRole() != Role.GARAGE_OWNER) {
            return base;
        }
        Set<String> merged = mechanicProfileRepository.findByGarageOwner(profile.getUser()).stream()
            .map(MechanicProfile::getSpeciality)
            .filter(Objects::nonNull)
            .filter(v -> !v.isBlank())
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!base.isBlank()) {
            merged.add(base);
        }
        return String.join(", ", merged);
    }

    private String resolveAggregatedExpertise(MechanicProfile profile) {
        String base = safe(profile.getExpertise());
        if (profile.getUser().getRole() != Role.GARAGE_OWNER) {
            return base;
        }
        Set<String> merged = mechanicProfileRepository.findByGarageOwner(profile.getUser()).stream()
            .map(worker -> {
                String expertise = safe(worker.getExpertise());
                return expertise.isBlank() ? safe(worker.getSpeciality()) : expertise;
            })
            .filter(v -> !v.isBlank())
            .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
        if (!base.isBlank()) {
            merged.add(base);
        }
        return String.join(", ", merged);
    }
}
