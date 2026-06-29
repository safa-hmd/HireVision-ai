package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.UserDTO;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final UserRepository userRepository;

    @Override
    public UserDTO getById(Long id) {
        return UserDTO.fromEntity(
                userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("User introuvable : " + id)));
    }

    @Override
    public List<UserDTO> getAll() {
        return userRepository.findAll()
                .stream().map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public UserDTO update(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User introuvable : " + id));
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        if (dto.getAge() > 0) user.setAge(dto.getAge());
        if (dto.getPhone() != null) user.setPhone(dto.getPhone());
        if (dto.getTitle() != null) user.setTitle(dto.getTitle());
        if (dto.getLinkedin() != null) user.setLinkedin(dto.getLinkedin());
        if (dto.getGithub() != null) user.setGithub(dto.getGithub());

        return UserDTO.fromEntity(userRepository.save(user));
    }

    @Override
    public void delete(Long id) {
        if (!userRepository.existsById(id))
            throw new RuntimeException("User introuvable : " + id);
        userRepository.deleteById(id);
    }

    // UserServiceImpl.java — implémenter
    @Override
    public UserDTO uploadPicture(Long id, MultipartFile file) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User introuvable : " + id));
        try {
            String filename = "user_" + id + "_" + System.currentTimeMillis()
                    + "_" + file.getOriginalFilename();
            Path uploadDir = Paths.get("uploads/pictures/");
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            user.setProfilePicture(filename);
            return UserDTO.fromEntity(userRepository.save(user));
        } catch (IOException e) {
            throw new RuntimeException("Erreur upload image : " + e.getMessage());
        }
    }
}
