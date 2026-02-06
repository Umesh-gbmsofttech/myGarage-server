package com.gbm.app.service;

import java.util.List;

import org.springframework.stereotype.Service;

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

    private final LiveLocationRepository liveLocationRepository;
    private final BookingRepository bookingRepository;

    public LiveLocation updateLocation(User user, Long bookingId, LocationUpdateRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getOwner().getId().equals(user.getId()) && !booking.getMechanic().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }

        LiveLocation location = liveLocationRepository.findByBookingIdAndUserId(bookingId, user.getId())
            .orElseGet(() -> {
                LiveLocation created = new LiveLocation();
                created.setBooking(booking);
                created.setUser(user);
                return created;
            });

        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        return liveLocationRepository.save(location);
    }

    public List<LiveLocation> getLocations(User user, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
        if (!booking.getOwner().getId().equals(user.getId()) && !booking.getMechanic().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return liveLocationRepository.findByBookingId(bookingId);
    }
}
