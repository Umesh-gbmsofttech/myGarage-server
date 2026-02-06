package com.gbm.app.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gbm.app.entity.Review;
import com.gbm.app.entity.ReviewType;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByTypeOrderByCreatedAtDesc(ReviewType type);
    List<Review> findByMechanicIdOrderByCreatedAtDesc(Long mechanicId);
}
