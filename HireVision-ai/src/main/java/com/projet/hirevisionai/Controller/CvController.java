package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.CvDTO;
import com.projet.hirevisionai.ServiceInterface.ICvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("cvs")
@RequiredArgsConstructor
public class CvController {

    private final ICvService cvService;

    @PostMapping("/upload")
    public ResponseEntity<CvDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId) {
        return ResponseEntity.ok(cvService.upload(file, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CvDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(cvService.getById(id));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<CvDTO>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(cvService.getByUserId(userId));
    }

    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<CvDTO> getLatest(@PathVariable Long userId) {
        return ResponseEntity.ok(cvService.getLatestByUserId(userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cvService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
