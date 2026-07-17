package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.SettingsDTO;

public interface ISettingsService {
    SettingsDTO getSettings();
    SettingsDTO updateSettings(SettingsDTO dto);
}
