package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.UserDTO;
import com.projet.hirevisionai.ServiceInterface.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAll() {
        return ResponseEntity.ok(userService.getAll());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
