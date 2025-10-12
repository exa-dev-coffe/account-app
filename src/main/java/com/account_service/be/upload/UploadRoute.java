package com.account_service.be.upload;

import com.account_service.be.annotation.RequireAuth;
import com.account_service.be.upload.dto.UploadResponseDto;
import com.account_service.be.utils.commons.ResponseModel;
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

    @DeleteMapping("/delete-profile")
    @RequireAuth
    public ResponseEntity<ResponseModel<String>> deleteFile(@RequestParam("url") String objectName) {
        return uploadService.deleteFile(objectName);
    }
}
