package com.gbm.app.service;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;

import javax.sql.rowset.serial.SerialBlob;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.gbm.app.dto.ImageResponse;
import com.gbm.app.entity.Image;
import com.gbm.app.repository.ImageRepository;
import com.gbm.app.repository.ImageRepository.ImageContentView;
import com.gbm.app.repository.ImageRepository.ImageMetadataView;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "application/pdf");

    private final ImageRepository imageRepository;
    private final ImageUrlService imageUrlService;

    @Value("${app.image.max-size-bytes:10485760}")
    private long maxImageSizeBytes;

    @Transactional
    public ImageResponse upload(MultipartFile file, String referenceType, Long referenceId) {
        validateFile(file);
        validateReference(referenceType, referenceId);

        Image image = new Image();
        image.setFileName(resolveFileName(file));
        image.setContentType(normalizedContentType(file));
        image.setSize(file.getSize());
        image.setReferenceType(referenceType.trim().toUpperCase(Locale.ROOT));
        image.setReferenceId(referenceId);
        image.setImageData(toBlob(file));

        Image saved = imageRepository.save(image);
        return toResponse(saved);
    }

    @Transactional
    public ImageResponse update(Long id, MultipartFile file, String referenceType, Long referenceId) {
        validateFile(file);
        validateReference(referenceType, referenceId);

        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        image.setFileName(resolveFileName(file));
        image.setContentType(normalizedContentType(file));
        image.setSize(file.getSize());
        image.setReferenceType(referenceType.trim().toUpperCase(Locale.ROOT));
        image.setReferenceId(referenceId);
        image.setImageData(toBlob(file));

        return toResponse(imageRepository.save(image));
    }

    @Transactional
    public Long upsertImageId(Long existingImageId, MultipartFile file, String referenceType, Long referenceId) {
        if (existingImageId == null) {
            return upload(file, referenceType, referenceId).getId();
        }
        return update(existingImageId, file, referenceType, referenceId).getId();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(Long id) {
        ImageContentView content = imageRepository.findContentById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));

        try {
            Blob blob = content.getImageData();
            long length = blob.length();
            InputStream inputStream = blob.getBinaryStream();
            InputStreamResource resource = new InputStreamResource(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(content.getContentType()));
            headers.setContentDisposition(ContentDisposition.inline().filename(content.getFileName()).build());
            headers.setContentLength(length);
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to read image data");
        }
    }

    @Transactional(readOnly = true)
    public Page<ImageResponse> list(Pageable pageable) {
        return imageRepository.findAllMetadata(pageable).map(this::toResponse);
    }

    @Transactional
    public void delete(Long id) {
        if (!imageRepository.existsById(id)) {
            throw new IllegalArgumentException("Image not found");
        }
        imageRepository.deleteById(id);
    }

    @Transactional
    public void deleteIfPresent(Long id) {
        if (id == null) {
            return;
        }
        imageRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public ImageResponse getMetadata(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Image not found"));
        return toResponse(image);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > maxImageSizeBytes) {
            throw new IllegalArgumentException("File exceeds max size");
        }
        String contentType = normalizedContentType(file);
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type");
        }
    }

    private void validateReference(String referenceType, Long referenceId) {
        if (referenceType == null || referenceType.isBlank()) {
            throw new IllegalArgumentException("referenceType is required");
        }
        if (referenceId == null) {
            throw new IllegalArgumentException("referenceId is required");
        }
    }

    private String normalizedContentType(MultipartFile file) {
        return file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
    }

    private String resolveFileName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return "image";
        }
        return originalName;
    }

    private Blob toBlob(MultipartFile file) {
        try {
            return new SerialBlob(file.getBytes());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to process file");
        }
    }

    private ImageResponse toResponse(Image image) {
        ImageResponse response = new ImageResponse();
        response.setId(image.getId());
        response.setFileName(image.getFileName());
        response.setContentType(image.getContentType());
        response.setSize(image.getSize());
        response.setReferenceType(image.getReferenceType());
        response.setReferenceId(image.getReferenceId());
        response.setImageUrl(imageUrlService.toImageUrl(image.getId()));
        response.setCreatedAt(image.getCreatedAt());
        response.setUpdatedAt(image.getUpdatedAt());
        return response;
    }

    private ImageResponse toResponse(ImageMetadataView image) {
        ImageResponse response = new ImageResponse();
        response.setId(image.getId());
        response.setFileName(image.getFileName());
        response.setContentType(image.getContentType());
        response.setSize(image.getSize());
        response.setReferenceType(image.getReferenceType());
        response.setReferenceId(image.getReferenceId());
        response.setImageUrl(imageUrlService.toImageUrl(image.getId()));
        response.setCreatedAt(image.getCreatedAt());
        response.setUpdatedAt(image.getUpdatedAt());
        return response;
    }
}
