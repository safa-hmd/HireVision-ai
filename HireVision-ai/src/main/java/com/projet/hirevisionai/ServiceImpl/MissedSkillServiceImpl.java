package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.MissedSkillDTO;
import com.projet.hirevisionai.Repository.MissedSkillRepository;
import com.projet.hirevisionai.ServiceInterface.IMissedSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MissedSkillServiceImpl implements IMissedSkillService {

    private final MissedSkillRepository missedSkillRepository;

    @Override
    public List<MissedSkillDTO> getByUserId(Long userId) {
        return missedSkillRepository.findByMatchingResultCvUserIdUser(userId)
                .stream().map(MissedSkillDTO::fromEntity)
                .collect(Collectors.toList());
    }
}