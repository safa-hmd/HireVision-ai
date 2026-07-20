package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.CvDTO;
import com.projet.hirevisionai.Dto.CvUploadResponseDTO;
import com.projet.hirevisionai.Entity.Role;
import com.projet.hirevisionai.Entity.User;
import com.projet.hirevisionai.Repository.UserRepository;
import com.projet.hirevisionai.ServiceInterface.ICvService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.projet.hirevisionai.Security.jwt.JwtAuthFilter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests controller pour /cvs.
 * checkOwnership() lit SecurityContextHolder.getContext().getAuthentication().getName(),
 * qui est peuplé par @WithMockUser(username = "...") indépendamment de la chaîne de filtres
 * (spring-security-test l'injecte via TestSecurityContextHolder).
 */
@WebMvcTest(controllers = CvController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@AutoConfigureMockMvc(addFilters = false)
class CvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ICvService cvService;

    @MockitoBean
    private UserRepository userRepository;

    private User owner(Long id) {
        return User.builder().idUser(id).email("owner@test.com").role(Role.CANDIDATE).build();
    }

    private User admin() {
        return User.builder().idUser(99L).email("admin@test.com").role(Role.ADMIN).build();
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void upload_shouldReturnCv_whenOwnerUploadsOwnCv() throws Exception {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));
        CvDTO dto = CvDTO.builder().id(10L).userId(1L).filePath("cv.pdf").build();
        when(cvService.upload(any(), eq(1L))).thenReturn(dto);

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/cvs/upload").file(file).param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void upload_shouldReturnForbidden_whenUploadingForAnotherUser() throws Exception {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/cvs/upload").file(file).param("userId", "2"))
                .andExpect(status().isForbidden());

        verify(cvService, never()).upload(any(), any());
    }

    @Test
    @WithMockUser(username = "admin@test.com")
    void getById_shouldReturnCv_whenRequesterIsAdmin() throws Exception {
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin()));
        CvDTO dto = CvDTO.builder().id(10L).userId(1L).filePath("cv.pdf").build();
        when(cvService.getById(10L)).thenReturn(dto);

        mockMvc.perform(get("/cvs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @WithMockUser(username = "intruder@test.com")
    void getById_shouldReturnForbidden_whenRequesterIsNotOwnerNorAdmin() throws Exception {
        when(userRepository.findByEmail("intruder@test.com")).thenReturn(Optional.of(owner(2L)));
        CvDTO dto = CvDTO.builder().id(10L).userId(1L).filePath("cv.pdf").build();
        when(cvService.getById(10L)).thenReturn(dto);

        mockMvc.perform(get("/cvs/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void getByUserId_shouldReturnCvs_whenOwnerRequestsOwnList() throws Exception {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));
        when(cvService.getByUserId(1L)).thenReturn(List.of(
                CvDTO.builder().id(1L).userId(1L).build(),
                CvDTO.builder().id(2L).userId(1L).build()
        ));

        mockMvc.perform(get("/cvs/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void delete_shouldReturnNoContent_whenOwnerDeletesOwnCv() throws Exception {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));
        CvDTO dto = CvDTO.builder().id(10L).userId(1L).build();
        when(cvService.getById(10L)).thenReturn(dto);

        mockMvc.perform(delete("/cvs/10"))
                .andExpect(status().isNoContent());

        verify(cvService).delete(10L);
    }

    @Test
    @WithMockUser(username = "owner@test.com")
    void uploadAndAnalyze_shouldReturnAnalysis_whenFileIsValid() throws Exception {
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(owner(1L)));
        CvUploadResponseDTO response = CvUploadResponseDTO.builder()
                .cv(CvDTO.builder().id(5L).userId(1L).build())
                .build();
        when(cvService.uploadAndAnalyze(any(), eq(1L))).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/cvs/upload-and-analyze").file(file).param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cv.id").value(5));
    }
}