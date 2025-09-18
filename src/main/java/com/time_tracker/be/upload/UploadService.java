package com.time_tracker.be.upload;

import com.time_tracker.be.common.ResponseModel;
import com.time_tracker.be.exception.BadRequestException;
import com.time_tracker.be.upload.dto.UploadResponseDto;
import com.time_tracker.be.utils.MinioUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class UploadService {
    private final MinioUtils minioUtils;

    public UploadService(MinioUtils minioUtils) {


        this.minioUtils = minioUtils;
    }

    public ResponseEntity<ResponseModel<UploadResponseDto>> uploadProfile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File tidak ditemukan");
        }
        try {
            String objectName = "coffe/images/profiles/" + System.currentTimeMillis() + file.getOriginalFilename();
            String url = minioUtils.uploadFile(file, objectName);
            UploadResponseDto data = new UploadResponseDto();
            data.setUrl(url);
            ResponseModel<UploadResponseDto> response = new ResponseModel<>(true, "Upload berhasil", data);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(response);
        } catch (Exception e) {
            log.error("Error upload file: {}", e.getMessage());
            throw new BadRequestException("Gagal mengupload file");
        }
    }
}
