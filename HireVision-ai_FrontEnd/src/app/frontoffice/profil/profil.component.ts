import {
  AfterViewInit, Component, ElementRef,
  OnInit, ViewChild
} from '@angular/core';
import { UserService, UserDTO } from '../../services/user.service';
import { AuthService } from '../../services/auth.service';
import { CvService, CvDTO } from '../../services/cv.service';
import Cropper from 'cropperjs';

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

  // ── Photo de profil ──
  previewUrl: string | null = null;
  selectedFile: File | null = null;

  // ── Crop ──
  showCropModal = false;
  rawImageUrl: string | null = null;
  private cropper: any = null;

  // ── Suppression compte ──
  showDeleteModal = false;
  deleteConfirmName = '';
  deleteNameMatch = false;
  deleteNameError = '';

  @ViewChild('pictureInput') pictureInput!: ElementRef<HTMLInputElement>;
  @ViewChild('cropperImage') cropperImage!: ElementRef<HTMLImageElement>;

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private cvService: CvService
  ) {}

  ngOnInit(): void { this.loadUserData(); }

  ngAfterViewInit(): void {
    initTabs();
    animateNumbers();
    lucide.createIcons();
  }

  // ─────────────────────────────────────────
  // Chargement données
  // ─────────────────────────────────────────
  loadUserData(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) return;

    this.userService.getById(userId).subscribe({
      next: (data) => { this.user = data; },
      error: () => showToast('Erreur chargement profil', 'danger')
    });

    this.cvService.getByUserId(userId).subscribe({
      next: (cvs) => { this.userCvs = cvs; },
      error: () => showToast('Erreur chargement CVs', 'danger')
    });
  }

  // ─────────────────────────────────────────
  // Photo de profil
  // ─────────────────────────────────────────
  getInitials(): string {
    if (!this.user.fullName) return '?';
    return this.user.fullName
      .split(' ').map(n => n[0]).join('').toUpperCase().slice(0, 2);
  }

  getProfilePicUrl(): string {
    return this.userService.getPictureUrl(this.user.profilePicture!);
  }

  triggerFileInput(): void {
    this.pictureInput.nativeElement.click();
  }

  // Quand l'user sélectionne un fichier → ouvrir le modal crop
  onPictureSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.[0]) return;

    const file = input.files[0];

    if (file.size > 5 * 1024 * 1024) {
      showToast('Image trop lourde (max 5 Mo)', 'danger');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      this.rawImageUrl = e.target?.result as string;
      this.showCropModal = true;

      // Initialiser Cropper après que Angular rende le modal
      setTimeout(() => this.initCropper(), 150);
    };
    reader.readAsDataURL(file);
  }

  private initCropper(): void {
    const imgEl = document.getElementById('cropper-image') as HTMLImageElement;
    if (!imgEl) return;

    // Détruire l'ancien cropper si existant
    if (this.cropper) {
      this.cropper.destroy();
      this.cropper = null;
    }

    this.cropper = new Cropper(imgEl, {
      aspectRatio: 1,           // carré = cercle
      viewMode: 1,              // empêche de sortir du canvas
      dragMode: 'move',         // déplacer l'image
      autoCropArea: 0.8,        // zone de crop = 80% du canvas
      cropBoxResizable: false,  // taille fixe, user déplace l'image
      cropBoxMovable: false,
      toggleDragModeOnDblclick: false,
      background: false,
    });
  }

  zoomIn(): void  { this.cropper?.zoom(0.1); }
  zoomOut(): void { this.cropper?.zoom(-0.1); }
  rotateLeft(): void  { this.cropper?.rotate(-90); }
  rotateRight(): void { this.cropper?.rotate(90); }

  // Confirmer le crop → générer le blob → preview
  applyCrop(): void {
    if (!this.cropper) return;

    const canvas = this.cropper.getCroppedCanvas({
      width: 300,
      height: 300,
      imageSmoothingEnabled: true,
      imageSmoothingQuality: 'high',
    });

    canvas.toBlob((blob: Blob | null) => {
      if (!blob) return;

      // Convertir blob en File
      this.selectedFile = new File([blob], 'profile.jpg', { type: 'image/jpeg' });

      // Preview locale
      this.previewUrl = canvas.toDataURL('image/jpeg', 0.9);

      // Fermer le modal crop
      this.showCropModal = false;
      this.cropper.destroy();
      this.cropper = null;
      this.rawImageUrl = null;

      lucide.createIcons();
      showToast('Photo prête — cliquez "Confirmer" pour sauvegarder', 'info');
    }, 'image/jpeg', 0.9);
  }

  cancelCrop(): void {
    if (this.cropper) { this.cropper.destroy(); this.cropper = null; }
    this.showCropModal = false;
    this.rawImageUrl = null;
    this.pictureInput.nativeElement.value = '';
  }

  // Upload vers le backend
  confirmUpload(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId || !this.selectedFile) return;

    this.userService.uploadPicture(userId, this.selectedFile).subscribe({
      next: (updated) => {
        this.user = updated;
        this.previewUrl = null;
        this.selectedFile = null;
        showToast('Photo mise à jour !', 'success');
        lucide.createIcons();
      },
      error: () => showToast("Erreur lors de l'upload", 'danger')
    });
  }

  cancelPreview(): void {
    this.previewUrl = null;
    this.selectedFile = null;
    this.pictureInput.nativeElement.value = '';
  }

  // ─────────────────────────────────────────
  // Profil & CV
  // ─────────────────────────────────────────
  saveProfile(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) return;
    this.userService.update(userId, this.user).subscribe({
      next: (updated) => { this.user = updated; showToast('Profil mis à jour !', 'success'); },
      error: () => showToast('Erreur mise à jour', 'danger')
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.[0]) return;
    const userId = this.authService.getCurrentUserId();
    if (!userId) return;
    showToast('Upload du CV...', 'info');
    this.cvService.upload(input.files[0], userId).subscribe({
      next: () => { showToast('CV uploadé !', 'success'); this.loadUserData(); },
      error: () => showToast("Erreur upload CV", 'danger')
    });
  }

  deleteCv(id?: number): void {
    if (!id || !confirm('Voulez-vous vraiment supprimer ce CV ?')) return;
    this.cvService.delete(id).subscribe({
      next: () => { showToast('CV supprimé !', 'success'); this.loadUserData(); },
      error: () => showToast('Erreur suppression', 'danger')
    });
  }

  uploadCv(): void {
    (document.getElementById('new-cv-file') as HTMLInputElement)?.click();
  }

  // ─────────────────────────────────────────
  // Suppression compte
  // ─────────────────────────────────────────
  openDeleteModal(): void {
    this.showDeleteModal = true;
    this.deleteConfirmName = '';
    this.deleteNameMatch = false;
    this.deleteNameError = '';
  }

  closeDeleteModal(): void { this.showDeleteModal = false; }

  onDeleteNameChange(): void {
    if (this.deleteConfirmName === this.user.fullName) {
      this.deleteNameMatch = true;
      this.deleteNameError = '';
    } else {
      this.deleteNameMatch = false;
      if (this.deleteConfirmName.length >= this.user.fullName.length) {
        this.deleteNameError = 'Le nom ne correspond pas. Vérifiez les majuscules.';
      } else {
        this.deleteNameError = '';
      }
    }
  }

  deleteAccount(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId || !this.deleteNameMatch) return;
    this.userService.delete(userId).subscribe({
      next: () => {
        showToast('Compte supprimé', 'success');
        this.authService.logout();
      },
      error: () => showToast('Erreur suppression compte', 'danger')
    });
  }
}