package com.gbm.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gbm.app.entity.MechanicProfile;

public interface MechanicProfileRepository extends JpaRepository<MechanicProfile, Long> {
    Optional<MechanicProfile> findByUserId(Long userId);
    List<MechanicProfile> findByVisibleTrue();
    Page<MechanicProfile> findByVisibleTrue(Pageable pageable);

    @Query("""
        SELECT m
        FROM MechanicProfile m
        WHERE m.visible = true
          AND (
            LOWER(CONCAT(COALESCE(m.user.firstName, ''), ' ', COALESCE(m.user.surname, ''))) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(m.speciality, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(m.expertise, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(COALESCE(m.city, '')) LIKE LOWER(CONCAT('%', :query, '%'))
          )
    """)
    Page<MechanicProfile> searchVisible(@Param("query") String query, Pageable pageable);
}
