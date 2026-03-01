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

    @Query(
        value = """
            SELECT mp.*
            FROM mechanic_profiles mp
            JOIN users u ON u.id = mp.user_id
            WHERE mp.visible = true
              AND (
                LOWER(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.surname, ''))) LIKE CONCAT('%', :query, '%')
                OR LOWER(COALESCE(mp.speciality, '')) LIKE CONCAT('%', :query, '%')
                OR LOWER(COALESCE(mp.expertise, '')) LIKE CONCAT('%', :query, '%')
                OR LOWER(COALESCE(mp.city, '')) LIKE CONCAT('%', :query, '%')
              )
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM mechanic_profiles mp
            JOIN users u ON u.id = mp.user_id
            WHERE mp.visible = true
              AND (
                LOWER(CONCAT(COALESCE(u.first_name, ''), ' ', COALESCE(u.surname, ''))) LIKE CONCAT('%', :query, '%')
                OR LOWER(COALESCE(mp.speciality, '')) LIKE CONCAT('%', :query, '%')
                OR LOWER(COALESCE(mp.expertise, '')) LIKE CONCAT('%', :query, '%')
                OR LOWER(COALESCE(mp.city, '')) LIKE CONCAT('%', :query, '%')
              )
            """,
        nativeQuery = true
    )
    Page<MechanicProfile> searchVisible(@Param("query") String query, Pageable pageable);
}
