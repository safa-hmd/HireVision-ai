package com.projet.hirevisionai.Dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class CvUploadRequestDTO {
    @NotNull
    private MultipartFile file;

    private Long userId;
}