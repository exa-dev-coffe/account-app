package com.time_tracker.be.upload;

import com.time_tracker.be.annotation.RequireAuth;
import com.time_tracker.be.common.ResponseModel;
import com.time_tracker.be.upload.dto.UploadResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
}
