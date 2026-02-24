package com.gbm.app.repository;

import java.sql.Blob;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.gbm.app.entity.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {

    interface ImageMetadataView {
        Long getId();
        String getFileName();
        String getContentType();
        Long getSize();
        String getReferenceType();
        Long getReferenceId();
        LocalDateTime getCreatedAt();
        LocalDateTime getUpdatedAt();
    }

    interface ImageContentView {
        String getFileName();
        String getContentType();
        Blob getImageData();
    }

    @Query("""
            select i.id as id, i.fileName as fileName, i.contentType as contentType, i.size as size,
                   i.referenceType as referenceType, i.referenceId as referenceId,
                   i.createdAt as createdAt, i.updatedAt as updatedAt
            from Image i
            """)
    Page<ImageMetadataView> findAllMetadata(Pageable pageable);

    @Query("""
            select i.fileName as fileName, i.contentType as contentType, i.imageData as imageData
            from Image i
            where i.id = :id
            """)
    Optional<ImageContentView> findContentById(@Param("id") Long id);
}
