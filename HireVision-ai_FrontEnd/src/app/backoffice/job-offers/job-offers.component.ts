import { AfterViewInit, Component, OnInit } from '@angular/core';
import { JobOfferService, JobOfferDTO } from '../../services/job-offer.service';

declare const lucide: any;
declare function showToast(message: string, type?: string): void;
declare function confirmAction(message: string, onConfirm: () => void): void;

@Component({
  selector: 'app-job-offers',
  templateUrl: './job-offers.component.html',
  styleUrls: ['./job-offers.component.css']
})
export class JobOffersComponent implements OnInit, AfterViewInit {

  offers: JobOfferDTO[] = [];
  isLoading = false;
  isModalOpen = false;
  isEditing = false;

  // Pagination
  currentPage = 1;
  pageSize = 5;

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.offers.length / this.pageSize));
  }

  // Formulaire (skills saisies séparées par des virgules)
  form: JobOfferDTO = this.emptyForm();
  skillsInput = '';
  editingId: number | null = null;

  constructor(private jobOfferService: JobOfferService) {}

  ngOnInit(): void {
    this.loadOffers();
  }

  ngAfterViewInit(): void {
    lucide.createIcons();
  }

  emptyForm(): JobOfferDTO {
    return { title: '', company: '', description: '', active: true, requiredSkills: [] };
  }

  loadOffers(): void {
    this.isLoading = true;
    this.jobOfferService.getAll().subscribe({
      next: (offers) => {
        this.offers = offers;
        this.currentPage = 1;
        this.isLoading = false;
        setTimeout(() => lucide.createIcons(), 50);
      },
      error: () => {
        this.isLoading = false;
        showToast("Erreur lors du chargement des offres", 'danger');
      }
    });
  }

  openCreateModal(): void {
    this.isEditing = false;
    this.editingId = null;
    this.form = this.emptyForm();
    this.skillsInput = '';
    this.isModalOpen = true;
  }

  openEditModal(offer: JobOfferDTO): void {
    this.isEditing = true;
    this.editingId = offer.id!;
    this.form = { ...offer };
    this.skillsInput = (offer.requiredSkills || []).join(', ');
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
  }

  save(): void {
    if (!this.form.title.trim()) {
      showToast('Le titre du poste est obligatoire', 'warning');
      return;
    }

    this.form.requiredSkills = this.skillsInput
      .split(',')
      .map(s => s.trim())
      .filter(s => s.length > 0);

    const request = this.isEditing
      ? this.jobOfferService.update(this.editingId!, this.form)
      : this.jobOfferService.create(this.form);

    request.subscribe({
      next: () => {
        showToast(this.isEditing ? 'Offre mise à jour' : 'Offre créée', 'success');
        this.isModalOpen = false;
        this.loadOffers();
      },
      error: () => showToast("Erreur lors de l'enregistrement", 'danger')
    });
  }

  deleteOffer(offer: JobOfferDTO): void {
    confirmAction(`Supprimer l'offre "${offer.title}" ?`, () => {
      this.jobOfferService.delete(offer.id!).subscribe({
        next: () => {
          showToast('Offre supprimée', 'success');
          this.loadOffers();
        },
        error: () => showToast('Erreur lors de la suppression', 'danger')
      });
    });
  }

  toggleActive(offer: JobOfferDTO): void {
    const updated = { ...offer, active: !offer.active };
    this.jobOfferService.update(offer.id!, updated).subscribe({
      next: () => {
        showToast(updated.active ? 'Offre publiée' : 'Offre dépubliée', 'success');
        this.loadOffers();
      },
      error: () => showToast('Erreur', 'danger')
    });
  }
}
