package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.UserDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IUserService {
    UserDTO getById(Long id);
    List<UserDTO> getAll();
    UserDTO update(Long id, UserDTO dto);
    void delete(Long id);
    UserDTO uploadPicture(Long id, MultipartFile file);
    Object analyzeGithub(Long id);
}
