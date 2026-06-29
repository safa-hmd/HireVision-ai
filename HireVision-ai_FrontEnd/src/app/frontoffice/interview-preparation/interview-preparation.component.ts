import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CvService } from '../../services/cv.service';

declare const lucide: any;

interface Category {
  id: string;
  title: string;
  description: string;
  difficulty: string;
  difficultyClass: string;
  duration: number;
  questions: number;
  color: string;
  icon: string;
  keywords: string[];
}

@Component({
  selector: 'app-interview-preparation',
  templateUrl: './interview-preparation.component.html',
  styleUrls: ['./interview-preparation.component.css']
})
export class InterviewPreparationComponent implements OnInit, AfterViewInit {

  hasCv = false;
  isLoading = true;
  userSkills: string[] = [];

  allCategories: Category[] = [
    {
      id: 'java', title: 'Programmation Java',
      description: 'Java Core, POO, Collections, Multithreading',
      difficulty: 'Intermédiaire', difficultyClass: 'diff-intermediate',
      duration: 45, questions: 15, color: '#2563EB', icon: 'code-2',
      keywords: ['java']
    },
    {
      id: 'spring', title: 'Spring Boot',
      description: 'REST API, Injection de dépendances, JPA',
      difficulty: 'Avancé', difficultyClass: 'diff-advanced',
      duration: 60, questions: 20, color: '#10B981', icon: 'sparkles',
      keywords: ['spring', 'spring boot', 'springboot']
    },
    {
      id: 'angular', title: 'Framework Angular',
      description: 'Composants, Services, RxJS, State Management',
      difficulty: 'Intermédiaire', difficultyClass: 'diff-intermediate',
      duration: 45, questions: 15, color: '#EF4444', icon: 'layers',
      keywords: ['angular', 'angularjs']
    },
    {
      id: 'ml', title: 'IA / Machine Learning',
      description: 'Algorithmes ML, Réseaux de neurones, Deep Learning',
      difficulty: 'Avancé', difficultyClass: 'diff-advanced',
      duration: 60, questions: 18, color: '#7C3AED', icon: 'brain',
      keywords: ['ml', 'machine learning', 'ia', 'ai', 'deep learning', 'python', 'tensorflow', 'pytorch', 'mlops', 'sklearn']
    },
    {
      id: 'devops', title: 'DevOps',
      description: 'Docker, Kubernetes, CI/CD, Cloud',
      difficulty: 'Intermédiaire', difficultyClass: 'diff-intermediate',
      duration: 45, questions: 15, color: '#06B6D4', icon: 'cloud',
      keywords: ['devops', 'docker', 'kubernetes', 'ci/cd', 'jenkins', 'ansible', 'terraform', 'cloud', 'aws', 'azure', 'gcp', 'linux']
    },
    {
      id: 'rh', title: 'Entretien RH',
      description: 'Questions comportementales, Soft skills, Culture fit',
      difficulty: 'Débutant', difficultyClass: 'diff-beginner',
      duration: 30, questions: 10, color: '#F59E0B', icon: 'users',
      keywords: ['rh', 'hr', 'soft skills', 'communication', 'management', 'leadership', 'agile', 'scrum']
    }
  ];

  filteredCategories: Category[] = [];

  constructor(
    private router: Router,
    private authService: AuthService,
    private cvService: CvService
  ) {}

  ngOnInit(): void { this.loadUserCvAndFilter(); }
  ngAfterViewInit(): void { setTimeout(() => lucide.createIcons(), 200); }

  loadUserCvAndFilter(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      this.isLoading = false; this.hasCv = false; this.filteredCategories = [];
      return;
    }

    this.cvService.getLatest(userId).subscribe({
      next: (cv) => {
        this.isLoading = false;
        if (cv && cv.skillNames && cv.skillNames.length > 0) {
          this.hasCv = true;
          this.userSkills = cv.skillNames;
          this.filterCategories();
        } else {
          this.hasCv = false;
          this.filteredCategories = [];
        }
        setTimeout(() => lucide.createIcons(), 100);
      },
      error: () => {
        this.isLoading = false; this.hasCv = false; this.filteredCategories = [];
        setTimeout(() => lucide.createIcons(), 100);
      }
    });
  }

  filterCategories(): void {
    this.filteredCategories = this.allCategories.filter(cat => {
      if (cat.id === 'rh') return true; // toujours visible
      return this.userSkills.some(skill =>
        cat.keywords.some(kw => skill.toLowerCase().includes(kw))
      );
    });

    // Si aucune correspondance (profil trop générique) → afficher tout
    if (this.filteredCategories.length <= 1) {
      this.filteredCategories = [...this.allCategories];
    }
  }

  startInterview(id: string, title: string, count: number, duration: number): void {
    sessionStorage.setItem('interview_specialty', JSON.stringify({ id, title, count, duration }));
    this.router.navigate(['/frontoffice/interview-session']);
  }

  goToCvUpload(): void { this.router.navigate(['/frontoffice/cvAnalyse']); }
}