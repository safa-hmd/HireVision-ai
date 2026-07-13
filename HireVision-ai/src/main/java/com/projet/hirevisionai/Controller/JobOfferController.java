package com.projet.hirevisionai.Controller;

import com.projet.hirevisionai.Dto.JobOfferDTO;
import com.projet.hirevisionai.ServiceInterface.IJobOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("job-offers")
@RequiredArgsConstructor
public class JobOfferController {

    private final IJobOfferService jobOfferService;

    @PostMapping
    public ResponseEntity<JobOfferDTO> create(@RequestBody JobOfferDTO dto) {
        return ResponseEntity.ok(jobOfferService.create(dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobOfferDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobOfferService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<JobOfferDTO>> getAll() {
        return ResponseEntity.ok(jobOfferService.getAll());
    }

    /** Utilisé côté candidat : uniquement les offres publiées/actives */
    @GetMapping("/active")
    public ResponseEntity<List<JobOfferDTO>> getActive() {
        return ResponseEntity.ok(jobOfferService.getActive());
    }

    @GetMapping("/search")
    public ResponseEntity<List<JobOfferDTO>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(jobOfferService.search(keyword));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobOfferDTO> update(@PathVariable Long id, @RequestBody JobOfferDTO dto) {
        return ResponseEntity.ok(jobOfferService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobOfferService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
