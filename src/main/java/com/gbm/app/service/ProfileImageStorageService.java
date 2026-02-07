package com.gbm.app.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import net.coobird.thumbnailator.Thumbnails;

@Service
public class ProfileImageStorageService {

    private final Path uploadDir;

    public ProfileImageStorageService(@Value("${app.profile.upload.dir}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String ext = getExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID().toString() + (ext.isBlank() ? ".jpg" : "." + ext);
        Path target = uploadDir.resolve(filename);

        try {
            Thumbnails.of(file.getInputStream())
                .size(600, 600)
                .outputQuality(0.8)
                .outputFormat(ext.isBlank() ? "jpg" : ext)
                .toFile(target.toFile());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to store file");
        }

        return "/uploads/profiles/" + filename;
    }

    public void delete(String urlPath) {
        if (urlPath == null || urlPath.isBlank()) {
            return;
        }
        String filename = urlPath.substring(urlPath.lastIndexOf('/') + 1);
        File file = uploadDir.resolve(filename).toFile();
        if (file.exists()) {
            file.delete();
        }
    }

    private String getExtension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }
}
