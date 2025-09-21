package com.time_tracker.be.upload;

import com.time_tracker.be.annotation.RequireAuth;
import com.time_tracker.be.upload.dto.UploadResponseDto;
import com.time_tracker.be.utils.commons.ResponseModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/1.0/upload")
public class UploadRoute {

    private final UploadService uploadService;

    public UploadRoute(UploadService uploadService) {
        this.uploadService = uploadService;
    }


    @PostMapping("/upload-profile")
    @RequireAuth
    public ResponseEntity<ResponseModel<UploadResponseDto>> uploadPhoto(@RequestParam("file") MultipartFile file) {
        return uploadService.uploadProfile(file);
    }

    @DeleteMapping("/delete")
    @RequireAuth
    public ResponseEntity<ResponseModel<String>> deleteFile(@RequestParam("url") String objectName) {
        return uploadService.deleteFile(objectName);
    }
}
