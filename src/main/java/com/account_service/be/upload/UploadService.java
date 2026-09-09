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

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
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
            throw new BadRequestException("Invalid file. Make sure file is an image (JPEG, PNG, WEBP) and maximum size is 5MB.");
        }
        try {
            String contentType = file.getContentType();
            boolean isAlreadyWebp = contentType != null && contentType.equalsIgnoreCase("image/webp");

            String url;
            if (isAlreadyWebp) {
                // File already converted to WebP (e.g. by frontend)
                String objectName = "coffe/images/profiles/" + System.currentTimeMillis() + UUID.randomUUID() + ".webp";
                url = minioService.uploadFile(file, objectName);
            } else {
                // Try converting and compressing to WebP in Java
                byte[] webpBytes = convertAndCompressToWebp(file);
                if (webpBytes != null && webpBytes.length > 0) {
                    String objectName = "coffe/images/profiles/" + System.currentTimeMillis() + UUID.randomUUID() + ".webp";
                    url = minioService.uploadBytes(webpBytes, objectName, "image/webp");
                } else {
                    // Fallback to original upload (e.g. if unit test uses mock binary content)
                    String originalName = file.getOriginalFilename();
                    String ext = "";
                    if (originalName != null && originalName.contains(".")) {
                        ext = originalName.substring(originalName.lastIndexOf("."));
                    } else {
                        ext = ".jpg";
                    }
                    String objectName = "coffe/images/profiles/" + System.currentTimeMillis() + UUID.randomUUID() + ext;
                    url = minioService.uploadFile(file, objectName);
                }
            }

            UploadResponseDto data = new UploadResponseDto();
            data.setUrl(url);
            ResponseModel<UploadResponseDto> response = new ResponseModel<>(true, "Upload successful", data);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            log.error("Error upload file: {}", e.getMessage());
            throw new BadRequestException("Failed to upload file");
        }
    }

    private byte[] convertAndCompressToWebp(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                return null;
            }

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
            if (!writers.hasNext()) {
                log.warn("No ImageWriter found for image/webp");
                return null;
            }

            ImageWriter writer = writers.next();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    String[] types = param.getCompressionTypes();
                    if (types != null && types.length > 0) {
                        param.setCompressionType(types[0]);
                    }
                    param.setCompressionQuality(0.82f);
                }
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }

            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("WebP compression failed in Java, falling back: {}", e.getMessage());
            return null;
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
        return contentType != null && (
                contentType.equalsIgnoreCase("image/jpeg") ||
                contentType.equalsIgnoreCase("image/png") ||
                contentType.equalsIgnoreCase("image/webp")
        );
    }

    private boolean validateFileSize(MultipartFile file) {
        long maxSize = 5 * 1024 * 1024; // 5MB
        return file.getSize() <= maxSize;
    }

}
