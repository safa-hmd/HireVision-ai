package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.CvDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ICvService {
    CvDTO upload(MultipartFile file, Long userId);
    CvDTO getById(Long id);
    List<CvDTO> getByUserId(Long userId);
    CvDTO getLatestByUserId(Long userId);
    void delete(Long id);
}