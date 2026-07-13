package com.projet.hirevisionai.Dto;

import com.projet.hirevisionai.Entity.MatchingResult;
import lombok.*;

import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchingResultDTO {

    private Long                 id;
    private float                score;
    private Long                 cvId;
    private Long                 jobOfferId;
    private String               jobOfferTitle;
    private List<MissedSkillDTO> missedSkills;
    private String        label;
    private String        message;
    private Boolean       compatible;
    private List<String>  matched;
    private List<String>  missingSkills;

    public static MatchingResultDTO fromEntity(MatchingResult mr) {
        if (mr == null) return null;
        return MatchingResultDTO.builder()
                .id(mr.getId())
                .score(mr.getScore())
                .cvId(mr.getCv() != null ? mr.getCv().getId() : null)
                .jobOfferId(mr.getJobOffer() != null ? mr.getJobOffer().getId() : null)
                .jobOfferTitle(mr.getJobOffer() != null ? mr.getJobOffer().getTitle() : null)
                .missedSkills(mr.getMissedSkills() != null
                        ? mr.getMissedSkills().stream()
                        .map(MissedSkillDTO::fromEntity)
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}