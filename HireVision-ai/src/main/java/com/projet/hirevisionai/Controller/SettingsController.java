package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.SettingsDTO;
import com.projet.hirevisionai.ServiceInterface.ISettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("settings")
@RequiredArgsConstructor
public class SettingsController {

    private final ISettingsService settingsService;

    @GetMapping
    public ResponseEntity<SettingsDTO> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<SettingsDTO> updateSettings(@RequestBody SettingsDTO dto) {
        return ResponseEntity.ok(settingsService.updateSettings(dto));
    }
}
