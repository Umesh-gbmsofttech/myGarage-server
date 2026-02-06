package com.gbm.app.service;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.gbm.app.dto.MechanicCardResponse;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.repository.MechanicProfileRepository;
import com.gbm.app.util.DistanceUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MechanicService {

    private final MechanicProfileRepository mechanicProfileRepository;
    private final Random random = new Random();

    public List<MechanicCardResponse> topRated(int limit) {
        return mechanicProfileRepository.findByVisibleTrue().stream()
            .sorted(Comparator.comparing(MechanicProfile::getRating, Comparator.nullsLast(Double::compareTo)).reversed())
            .limit(limit)
            .map(this::toCard)
            .collect(Collectors.toList());
    }

    public List<MechanicCardResponse> random(int limit) {
        List<MechanicProfile> profiles = mechanicProfileRepository.findByVisibleTrue();
        return profiles.stream()
            .sorted((a, b) -> random.nextInt(3) - 1)
            .limit(limit)
            .map(this::toCard)
            .collect(Collectors.toList());
    }

    public List<MechanicCardResponse> search(String query, Double lat, Double lng, double radiusKm) {
        String safeQuery = query == null ? "" : query.toLowerCase();
        return mechanicProfileRepository.findByVisibleTrue().stream()
            .filter(profile -> matches(profile, safeQuery))
            .map(profile -> withDistance(profile, lat, lng))
            .filter(card -> card.getDistanceKm() == null || card.getDistanceKm() <= radiusKm)
            .sorted(Comparator.comparing(MechanicCardResponse::getRating, Comparator.nullsLast(Double::compareTo)).reversed())
            .collect(Collectors.toList());
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
        String speciality = safe(profile.getSpeciality()).toLowerCase();
        String expertise = safe(profile.getExpertise()).toLowerCase();
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
        card.setSpeciality(profile.getSpeciality());
        card.setExpertise(profile.getExpertise());
        card.setRating(profile.getRating());
        card.setRatingCount(profile.getRatingCount());
        card.setCity(profile.getCity());
        card.setProfileImageUrl(profile.getProfileImageUrl());
        return card;
    }
}
