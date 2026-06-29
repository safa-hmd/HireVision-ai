import { Component, AfterViewInit } from '@angular/core';
import { CvService, CvDTO, CvAnalysis, CvUploadResponse } from '../../services/cv.service';
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
  isLoading = false;
  uploadedCv: CvDTO | null = null;
  analysis: CvAnalysis | null = null;
  fileName = '';

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

    this.fileName = file.name;
    this.isLoading = true;
    showToast('Analyse IA en cours...', 'info');

    this.cvService.uploadAndAnalyze(file, userId).subscribe({
      next: (res: CvUploadResponse) => {
        this.uploadedCv = res.cv;
        this.analysis = res.analysis;
        this.showResults = true;
        this.isLoading = false;
        setTimeout(() => lucide.createIcons(), 50);
        showToast('CV analysé avec succès !', 'success');
      },
      error: () => {
        this.isLoading = false;
        showToast("Erreur lors de l'analyse du CV", 'danger');
      }
    });
  }

  simulateUpload(): void {
    this.fileName = 'Jean_Dupont_CV.pdf';
    this.analysis = {
      skills: ['Java', 'Spring Boot', 'Microservices', 'REST API', 'Angular',
               'TypeScript', 'Docker', 'Kubernetes', 'MySQL', 'MongoDB', 'Git', 'CI/CD', 'Agile', 'Leadership'],
      education: [
        { degree: 'Master en Informatique', institution: 'Université Paris-Saclay', period: '2020 – 2022' },
        { degree: 'Licence Génie Logiciel', institution: 'INSA Lyon', period: '2017 – 2020' }
      ],
      experience: [
        {
          title: 'Ingénieur Logiciel Senior',
          company: 'Tech Corp SARL',
          period: '2022 – Présent',
          description: 'Architecture microservices pour 1 M+ utilisateurs'
        },
        {
          title: 'Développeur Full Stack',
          company: 'StartupXYZ',
          period: '2020 – 2022',
          description: 'Applications Java Spring + Angular'
        }
      ],
      certifications: [
        'AWS Certified Solutions Architect',
        'Oracle Certified Java Professional',
        'Certified Kubernetes Administrator',
        'Google Cloud Professional Developer'
      ],
      languages: [
        { language: 'Français', level: 'Natif' },
        { language: 'Anglais', level: 'Courant' },
        { language: 'Espagnol', level: 'Intermédiaire' },
        { language: 'Arabe', level: 'Notions' }
      ],
      summary: 'Ingénieur logiciel senior avec 4 ans d\'expérience en architecture microservices et développement full stack. Expert Java/Spring Boot et Angular avec de solides compétences en cloud et DevOps.'
    };
    this.showResults = true;
    setTimeout(() => lucide.createIcons(), 50);
    showToast('Analyse de démonstration chargée !', 'success');
  }

  resetUpload(): void {
    this.showResults = false;
    this.uploadedCv = null;
    this.analysis = null;
    this.fileName = '';
    this.isLoading = false;
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