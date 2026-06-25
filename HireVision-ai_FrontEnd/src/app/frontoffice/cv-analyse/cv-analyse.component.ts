import { Component, AfterViewInit } from '@angular/core';
import { CvService, CvDTO } from '../../services/cv.service';
import { AuthService } from '../../services/auth.service';

declare const lucide: any;
declare function showToast(msg: string, type?: string): void;

@Component({
  selector: 'app-cv-analyse',
  templateUrl: './cv-analyse.component.html',
  styleUrls: ['./cv-analyse.component.css']
})
export class CvAnalyseComponent implements AfterViewInit {

  showResults = false;
  uploadedCv: CvDTO | null = null;

  constructor(
    private cvService: CvService,
    private authService: AuthService
  ) {}

  ngAfterViewInit(): void {
    lucide.createIcons();
    this.initDropZone();
  }

  triggerFileInput(): void {
    const input = document.getElementById('cv-file') as HTMLInputElement;
    input?.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.uploadFile(input.files[0]);
    }
  }

  uploadFile(file: File): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      showToast('Utilisateur non connecté', 'danger');
      return;
    }

    showToast('Upload et analyse en cours...', 'info');

    this.cvService.upload(file, userId).subscribe({
      next: (res) => {
        this.uploadedCv = res;
        this.showResults = true;
        setTimeout(() => lucide.createIcons(), 50);
        showToast('CV téléversé et analysé avec succès !', 'success');
      },
      error: (err) => {
        showToast("Erreur lors de l'upload du CV", 'danger');
      }
    });
  }

  simulateUpload(): void {
    this.showResults = true;
    setTimeout(() => lucide.createIcons(), 50);
    showToast('Analyse de démonstration chargée !', 'success');
  }

  resetUpload(): void {
    this.showResults = false;
    this.uploadedCv = null;
    setTimeout(() => {
      lucide.createIcons();
      this.initDropZone();
    }, 50);
  }

  private initDropZone(): void {
    const dropZone = document.getElementById('drop-zone');
    if (!dropZone) return;

    dropZone.addEventListener('dragover', (e) => {
      e.preventDefault();
      dropZone.classList.add('dragging');
    });
    dropZone.addEventListener('dragleave', () => {
      dropZone.classList.remove('dragging');
    });
    dropZone.addEventListener('drop', (e: DragEvent) => {
      e.preventDefault();
      dropZone.classList.remove('dragging');
      const file = e.dataTransfer?.files[0];
      if (file) this.uploadFile(file);
    });
  }
}

