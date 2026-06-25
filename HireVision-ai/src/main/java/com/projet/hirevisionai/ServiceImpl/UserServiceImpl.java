package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.UserDTO;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        return UserDTO.fromEntity(userRepository.save(user));
    }

    @Override
    public void delete(Long id) {
        if (!userRepository.existsById(id))
            throw new RuntimeException("User introuvable : " + id);
        userRepository.deleteById(id);
    }
}
