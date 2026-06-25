import { AfterViewInit, Component, OnInit } from '@angular/core';
import { UserService, UserDTO } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { CvService, CvDTO } from '../../services/cv.service';

declare const lucide: any;
declare function animateNumbers(): void;
declare function initTabs(): void;
declare function showToast(message: string, type?: string): void;

@Component({
  selector: 'app-profil',
  templateUrl: './profil.component.html',
  styleUrls: ['./profil.component.css']
})
export class ProfilComponent implements OnInit, AfterViewInit {
  user: UserDTO = { fullName: '', email: '', age: 0 };
  userCvs: CvDTO[] = [];

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private cvService: CvService
  ) {}

  ngOnInit(): void {
    this.loadUserData();
  }

  ngAfterViewInit(): void {
    initTabs();
    animateNumbers();
    lucide.createIcons();
  }

  loadUserData(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) return;

    this.userService.getById(userId).subscribe({
      next: (data) => {
        this.user = data;
      },
      error: () => showToast('Erreur chargement profil', 'danger')
    });

    this.cvService.getByUserId(userId).subscribe({
      next: (cvs) => {
        this.userCvs = cvs;
      },
      error: () => showToast('Erreur chargement CVs', 'danger')
    });
  }

  saveProfile(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) return;

    this.userService.update(userId, this.user).subscribe({
      next: (updated) => {
        this.user = updated;
        showToast('Profil mis à jour avec succès !', 'success');
      },
      error: () => {
        showToast('Erreur lors de la mise à jour du profil', 'danger');
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      const userId = this.authService.getCurrentUserId();
      if (!userId) return;

      showToast('Uploade du CV...', 'info');
      this.cvService.upload(file, userId).subscribe({
        next: () => {
          showToast('CV uploade avec succès !', 'success');
          this.loadUserData();
        },
        error: () => showToast("Erreur lors de l'upload", 'danger')
      });
    }
  }

  deleteCv(id?: number): void {
    if (!id) return;
    if (confirm('Voulez-vous vraiment supprimer ce CV ?')) {
      this.cvService.delete(id).subscribe({
        next: () => {
          showToast('CV supprimé !', 'success');
          this.loadUserData();
        },
        error: () => showToast('Erreur lors de la suppression', 'danger')
      });
    }
  }

  uploadCv(): void {
    const fileInput = document.getElementById('new-cv-file') as HTMLInputElement;
    fileInput?.click();
  }
}

