package com.account_service.be.upload;

import com.account_service.be.exception.BadRequestException;
import com.account_service.be.lib.MinioService;
import com.account_service.be.upload.dto.UploadResponseDto;
import com.account_service.be.utils.commons.ResponseModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@Service
public class UploadService {
    private final MinioService minioService;

    public UploadService(MinioService minioService) {


        this.minioService = minioService;
    }

    public ResponseEntity<ResponseModel<UploadResponseDto>> uploadProfile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File not found");
        }
        if (!validateFile(file)) {
            throw new BadRequestException("Invalid file. Make sure file is an image (JPEG, PNG) and maximum size is 5MB.");
        }
        try {
            // random word  di ujungnya ambil extensionnya
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            } else {
                ext = ".jpg"; // default extension
            }
            String objectName = "coffe/images/profiles/" + System.currentTimeMillis() + UUID.randomUUID() + ext;
            String url = minioService.uploadFile(file, objectName);
            UploadResponseDto data = new UploadResponseDto();
            data.setUrl(url);
            ResponseModel<UploadResponseDto> response = new ResponseModel<>(true, "Upload successful", data);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        } catch (Exception e) {
            log.error("Error upload file: {}", e.getMessage());
            throw new BadRequestException("Failed to upload file");
        }
    }

    public ResponseEntity<ResponseModel<String>> deleteFile(String url) {
        if (url == null || url.isEmpty()) {
            throw new BadRequestException("URL not found");
        }
        if (!url.startsWith("https://storage.eka-dev.cloud/project")) {
            throw new BadRequestException("Invalid URL");
        }
        String objectName = url.split("project")[1].substring(1);
        minioService.deleteFile(objectName);
        ResponseModel<String> response = new ResponseModel<>(true, "Delete successful", objectName);
        return ResponseEntity.status(HttpStatus.OK)
                .body(response);
    }

    private boolean validateFile(MultipartFile file) {
        return validateFileType(file) && validateFileSize(file);
    }

    private boolean validateFileType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png"));
    }

    private boolean validateFileSize(MultipartFile file) {
        long maxSize = 5 * 1024 * 1024; // 5MB
        return file.getSize() <= maxSize;
    }

}
