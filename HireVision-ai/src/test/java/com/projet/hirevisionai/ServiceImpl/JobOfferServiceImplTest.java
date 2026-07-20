package com.projet.hirevisionai.ServiceImpl;

import com.projet.hirevisionai.Dto.JobOfferDTO;
import com.projet.hirevisionai.Entity.JobOffer;
import com.projet.hirevisionai.Entity.Skill;
import com.projet.hirevisionai.Repository.JobOfferRepository;
import com.projet.hirevisionai.Repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobOfferServiceImplTest {

    @Mock
    private JobOfferRepository jobOfferRepository;
    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private JobOfferServiceImpl jobOfferService;

    @Test
    void createTest_shouldResolveExistingSkills_andSave() {
        JobOfferDTO dto = JobOfferDTO.builder()
                .title("Développeur Java")
                .company("HireVision")
                .description("Poste backend")
                .requiredSkills(List.of("Java"))
                .build();

        Skill javaSkill = Skill.builder().id(1L).name("Java").category("Backend").build();
        when(skillRepository.findByName("Java")).thenReturn(Optional.of(javaSkill));
        when(jobOfferRepository.save(any(JobOffer.class))).thenAnswer(inv -> inv.getArgument(0));

        JobOfferDTO result = jobOfferService.create(dto);

        assertThat(result.getTitle()).isEqualTo("Développeur Java");
        assertThat(result.isActive()).isTrue();
        verify(skillRepository, never()).save(any());
    }

    @Test
    void createTest_shouldCreateMissingSkills() {
        JobOfferDTO dto = JobOfferDTO.builder()
                .title("Développeur Angular")
                .company("HireVision")
                .requiredSkills(List.of("Angular"))
                .build();

        when(skillRepository.findByName("Angular")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jobOfferRepository.save(any(JobOffer.class))).thenAnswer(inv -> inv.getArgument(0));

        JobOfferDTO result = jobOfferService.create(dto);

        assertThat(result.getRequiredSkills()).contains("Angular");
        verify(skillRepository).save(any(Skill.class));
    }

    @Test
    void getByIdTest_shouldReturn_whenFound() {
        JobOffer offer = JobOffer.builder().id(1L).title("Dev").active(true).build();
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));

        JobOfferDTO result = jobOfferService.getById(1L);

        assertThat(result.getTitle()).isEqualTo("Dev");
    }

    @Test
    void getByIdTest_shouldThrow_whenNotFound() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobOfferService.getById(1L)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAllTest_shouldReturnMappedList() {
        JobOffer offer = JobOffer.builder().id(1L).title("Dev").active(true).build();
        when(jobOfferRepository.findAll()).thenReturn(List.of(offer));

        assertThat(jobOfferService.getAll()).hasSize(1);
    }

    @Test
    void getActiveTest_shouldReturnOnlyActiveOffers() {
        JobOffer offer = JobOffer.builder().id(1L).title("Dev").active(true).build();
        when(jobOfferRepository.findByActiveTrue()).thenReturn(List.of(offer));

        assertThat(jobOfferService.getActive()).hasSize(1);
    }

    @Test
    void searchTest_shouldDelegateToRepository() {
        JobOffer offer = JobOffer.builder().id(1L).title("Dev Java").active(true).build();
        when(jobOfferRepository.findByTitleContainingIgnoreCase("java")).thenReturn(List.of(offer));

        assertThat(jobOfferService.search("java")).hasSize(1);
    }

    @Test
    void updateTest_shouldModifyOffer_whenFound() {
        JobOffer offer = JobOffer.builder().id(1L).title("Old title").active(true).build();
        JobOfferDTO dto = JobOfferDTO.builder()
                .title("New title")
                .company("HireVision")
                .description("desc")
                .active(false)
                .requiredSkills(List.of())
                .build();

        when(jobOfferRepository.findById(1L)).thenReturn(Optional.of(offer));
        when(jobOfferRepository.save(any(JobOffer.class))).thenAnswer(inv -> inv.getArgument(0));

        JobOfferDTO result = jobOfferService.update(1L, dto);

        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void updateTest_shouldThrow_whenNotFound() {
        when(jobOfferRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobOfferService.update(1L, JobOfferDTO.builder().build()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deleteTest_shouldRemove_whenExists() {
        when(jobOfferRepository.existsById(1L)).thenReturn(true);

        jobOfferService.delete(1L);

        verify(jobOfferRepository).deleteById(1L);
    }

    @Test
    void deleteTest_shouldThrow_whenNotExists() {
        when(jobOfferRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> jobOfferService.delete(1L)).isInstanceOf(RuntimeException.class);
    }
}
