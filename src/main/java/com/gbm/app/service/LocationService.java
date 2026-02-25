package com.gbm.app.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gbm.app.dto.LiveLocationDTO;
import com.gbm.app.dto.LiveLocationPointDTO;
import com.gbm.app.dto.LocationUpdateRequest;
import com.gbm.app.entity.Booking;
import com.gbm.app.entity.LiveLocation;
import com.gbm.app.entity.User;
import com.gbm.app.repository.BookingRepository;
import com.gbm.app.repository.LiveLocationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LocationService {

    private static final Logger logger = LoggerFactory.getLogger(LocationService.class);
    private static final long LIVE_LOCATION_CACHE_TTL_MS = 1_000L;
    private final LiveLocationRepository liveLocationRepository;
    private final BookingRepository bookingRepository;
    private final ConcurrentHashMap<String, CachedLiveLocationResponse> liveLocationCache = new ConcurrentHashMap<>();

    @Transactional
    public LiveLocationDTO updateLocation(User user, Long bookingId, LocationUpdateRequest request) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!bookingRepository.isUserInBooking(bookingId, user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        List<LiveLocation> existingLocations = liveLocationRepository.findByBookingIdAndUserId(bookingId, user.getId());
        LiveLocation location;
        if (existingLocations.isEmpty()) {
            location = new LiveLocation();
            Booking bookingRef = bookingRepository.getReferenceById(bookingId);
            location.setBooking(bookingRef);
            location.setUser(user);
        } else {
            location = existingLocations.get(0);
            if (existingLocations.size() > 1) {
                logger.warn(
                    "Duplicate live location rows found for bookingId={} userId={} count={}. Keeping id={}",
                    bookingId,
                    user.getId(),
                    existingLocations.size(),
                    location.getId()
                );
            }
        }

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        LiveLocation saved = liveLocationRepository.save(location);
        invalidateLiveLocationCache(bookingId);
        return LiveLocationDTO.of(saved.getBooking().getId(), saved.getLatitude(), saved.getLongitude(), saved.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<LiveLocationPointDTO> getLiveLocationPoints(User user, Long bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new IllegalArgumentException("Booking not found");
        }
        if (!bookingRepository.isUserInBooking(bookingId, user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        String key = cacheKey(user.getId(), bookingId);
        CachedLiveLocationResponse cached = liveLocationCache.get(key);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiresAtMs > now) {
            return cached.data;
        }

        List<LiveLocationPointDTO> points = liveLocationRepository.findLocationPointsByBookingId(bookingId);
        liveLocationCache.put(key, new CachedLiveLocationResponse(points, now + LIVE_LOCATION_CACHE_TTL_MS));
        return points;
    }

    private String cacheKey(Long userId, Long bookingId) {
        return String.format(Locale.US, "%d:%d", userId, bookingId);
    }

    private void invalidateLiveLocationCache(Long bookingId) {
        String suffix = ":" + bookingId;
        liveLocationCache.keySet().removeIf(key -> key.endsWith(suffix));
    }

    private static class CachedLiveLocationResponse {
        private final List<LiveLocationPointDTO> data;
        private final long expiresAtMs;

        private CachedLiveLocationResponse(List<LiveLocationPointDTO> data, long expiresAtMs) {
            this.data = data;
            this.expiresAtMs = expiresAtMs;
        }
    }
}
