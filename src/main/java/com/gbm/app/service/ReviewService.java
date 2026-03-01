package com.gbm.app.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.gbm.app.dto.ReviewRequest;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.entity.Review;
import com.gbm.app.entity.ReviewType;
import com.gbm.app.entity.User;
import com.gbm.app.repository.MechanicProfileRepository;
import com.gbm.app.repository.ReviewRepository;
import com.gbm.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final MechanicProfileRepository mechanicProfileRepository;

    public Review create(User author, ReviewRequest request) {
        Review review = new Review();
        review.setAuthor(author);
        review.setType(request.getType());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        if (request.getType() == ReviewType.MECHANIC) {
            if (request.getMechanicId() == null) {
                throw new IllegalArgumentException("Mechanic id required");
            }
            User mechanic = userRepository.findById(request.getMechanicId())
                .orElseThrow(() -> new IllegalArgumentException("Mechanic not found"));
            review.setMechanic(mechanic);
            updateMechanicRating(mechanic.getId(), request.getRating());
        }

        return reviewRepository.save(review);
    }

    public List<Review> listPlatformReviews() {
        return reviewRepository.findByTypeOrderByCreatedAtDesc(ReviewType.PLATFORM);
    }

    public Page<Review> listPlatformReviews(int page, int size) {
        return reviewRepository.findByTypeOrderByCreatedAtDesc(ReviewType.PLATFORM, PageRequest.of(page, size));
    }

    public List<Review> listMechanicReviews(Long mechanicId) {
        return reviewRepository.findByMechanicIdOrderByCreatedAtDesc(mechanicId);
    }

    public Page<Review> listMechanicReviews(Long mechanicId, int page, int size) {
        return reviewRepository.findByMechanicIdOrderByCreatedAtDesc(mechanicId, PageRequest.of(page, size));
    }

    private void updateMechanicRating(Long mechanicId, Integer rating) {
        MechanicProfile profile = mechanicProfileRepository.findByUserId(mechanicId)
            .orElseThrow(() -> new IllegalArgumentException("Mechanic profile not found"));
        int count = profile.getRatingCount() == null ? 0 : profile.getRatingCount();
        double current = profile.getRating() == null ? 0.0 : profile.getRating();
        double newRating = ((current * count) + rating) / (count + 1);
        profile.setRatingCount(count + 1);
        profile.setRating(newRating);
        mechanicProfileRepository.save(profile);
    }
}
