package com.projet.hirevisionai.ServiceInterface;

import com.projet.hirevisionai.Dto.JobOfferDTO;

import java.util.List;

public interface IJobOfferService {
    JobOfferDTO create(JobOfferDTO dto);
    JobOfferDTO getById(Long id);
    List<JobOfferDTO> getAll();
    List<JobOfferDTO> getActive();
    List<JobOfferDTO> search(String keyword);
    JobOfferDTO update(Long id, JobOfferDTO dto);
    void delete(Long id);
}
