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
  /** true = compétence détectée dynamiquement dans le CV, pas une des 6
   *  spécialités pré-câblées côté service Python. */
  isCustom?: boolean;
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

  /** Spécialités "curées" — pool de questions riche et déjà validé côté
   *  service Python. Utilisées en priorité quand une compétence du CV
   *  correspond à l'une d'elles. */
  knownCategories: Category[] = [
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
    }
  ];

  /** Toujours disponible, indépendamment du CV. */
  private readonly rhCategory: Category = {
    id: 'rh', title: 'Entretien RH',
    description: 'Questions comportementales, Soft skills, Culture fit',
    difficulty: 'Débutant', difficultyClass: 'diff-beginner',
    duration: 30, questions: 10, color: '#F59E0B', icon: 'users',
    keywords: ['rh', 'hr', 'soft skills', 'communication', 'management', 'leadership', 'agile', 'scrum']
  };

  filteredCategories: Category[] = [];

  /** Palette tournante pour les compétences "custom" détectées dans le CV,
   *  qui n'ont pas de couleur pré-assignée. */
  private readonly palette = [
    '#2563EB', '#10B981', '#EF4444', '#7C3AED', '#06B6D4',
    '#F59E0B', '#EC4899', '#84CC16', '#F97316', '#0EA5E9'
  ];

  /** Mapping mots-clés → icône lucide, pour donner une icône pertinente
   *  même à une compétence non pré-câblée. */
  private readonly iconMap: { keywords: string[]; icon: string }[] = [
    { keywords: ['react'],                                            icon: 'atom' },
    { keywords: ['vue'],                                              icon: 'layers' },
    { keywords: ['node', 'express', 'nestjs'],                        icon: 'server' },
    { keywords: ['docker', 'kubernetes', 'k8s', 'container'],         icon: 'container' },
    { keywords: ['aws', 'azure', 'gcp', 'cloud'],                     icon: 'cloud' },
    { keywords: ['sql', 'database', 'mysql', 'postgres', 'mongodb', 'nosql', 'redis'], icon: 'database' },
    { keywords: ['git'],                                              icon: 'git-branch' },
    { keywords: ['security', 'sécurité', 'cyber'],                    icon: 'shield' },
    { keywords: ['test', 'qa', 'junit', 'selenium', 'cypress'],       icon: 'check-circle' },
    { keywords: ['design', 'ux', 'ui', 'figma'],                      icon: 'palette' },
    { keywords: ['php', 'laravel', 'symfony'],                        icon: 'code-2' },
    { keywords: ['c#', '.net', 'dotnet', 'csharp'],                   icon: 'code-2' },
    { keywords: ['go', 'golang', 'rust'],                             icon: 'terminal-square' },
    { keywords: ['kotlin', 'swift', 'flutter', 'mobile', 'android', 'ios'], icon: 'smartphone' },
    { keywords: ['html', 'css', 'sass', 'tailwind'],                  icon: 'layout' },
    { keywords: ['typescript', 'javascript'],                         icon: 'file-code' },
    { keywords: ['graphql'],                                          icon: 'share-2' },
    { keywords: ['kafka', 'rabbitmq'],                                icon: 'shuffle' },
    { keywords: ['linux', 'unix', 'bash', 'shell'],                   icon: 'terminal' },
    { keywords: ['agile', 'scrum', 'management', 'leadership'],       icon: 'users' },
    { keywords: ['data', 'analytics', 'bi', 'power bi', 'tableau'],   icon: 'bar-chart-2' },
  ];

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
      // Pas d'utilisateur connu → impossible de savoir s'il a un CV.
      // On n'affiche AUCUNE carte pré-câblée par défaut.
      this.isLoading = false;
      this.hasCv = false;
      this.filteredCategories = [];
      return;
    }

    this.cvService.getLatest(userId).subscribe({
      next: (cv) => {
        this.isLoading = false;
        if (cv && cv.skillNames && cv.skillNames.length > 0) {
          this.hasCv = true;
          this.userSkills = cv.skillNames;
          this.buildCategories();
        } else {
          // CV absent ou sans compétences détectées → aucune carte,
          // seulement le message d'invitation à analyser un CV.
          this.hasCv = false;
          this.filteredCategories = [];
        }
        setTimeout(() => lucide.createIcons(), 100);
      },
      error: () => {
        // Erreur réseau/serveur → même comportement : pas de CV connu,
        // donc pas de fallback vers les 6 spécialités pré-câblées.
        this.isLoading = false;
        this.hasCv = false;
        this.filteredCategories = [];
        setTimeout(() => lucide.createIcons(), 100);
      }
    });
  }

  /**
   * Construit la liste des catégories à partir des VRAIES compétences du CV :
   * - les compétences qui correspondent à une spécialité connue (pool riche,
   *   déjà validé) utilisent cette spécialité ;
   * - TOUTES les autres compétences du CV (React, Docker, SQL, PHP, gestion
   *   de projet, peu importe) génèrent leur propre carte "custom", envoyée
   *   telle quelle à l'IA pour générer des questions pertinentes.
   * - "Entretien RH" reste toujours proposé.
   */
  private buildCategories(): void {
    const matchedKnown = this.knownCategories.filter(cat =>
      this.userSkills.some(skill => cat.keywords.some(kw => skill.toLowerCase().includes(kw)))
    );

    const customCategories: Category[] = [];
    const seenSlugs = new Set<string>();

    this.userSkills.forEach(rawSkill => {
      const skill = (rawSkill || '').trim();
      if (!skill) return;
      const lower = skill.toLowerCase();

      // Ignorer si déjà couverte par une spécialité connue (évite le doublon
      // "Java" carte connue + "Java" carte custom)
      const alreadyCovered = this.knownCategories.some(cat =>
        cat.keywords.some(kw => lower.includes(kw))
      );
      if (alreadyCovered) return;

      const slug = this.slugify(skill);
      if (seenSlugs.has(slug)) return;
      seenSlugs.add(slug);

      customCategories.push({
        id: slug,
        title: skill,
        description: `Questions techniques et mises en situation sur ${skill}`,
        difficulty: 'Intermédiaire',
        difficultyClass: 'diff-intermediate',
        duration: 45,
        questions: 15,
        color: this.palette[customCategories.length % this.palette.length],
        icon: this.pickIcon(lower),
        keywords: [lower],
        isCustom: true
      });
    });

    // Uniquement ce qui a été réellement détecté dans le CV (+ RH, toujours
    // disponible). Si le CV ne matche rien de connu, ses compétences brutes
    // partent quand même en cartes "custom" grâce à la boucle ci-dessus ;
    // on ne retombe JAMAIS sur les 6 spécialités pré-câblées par défaut.
    this.filteredCategories = [...matchedKnown, ...customCategories, this.rhCategory];
  }

  private pickIcon(lowerSkill: string): string {
    const match = this.iconMap.find(m => m.keywords.some(kw => lowerSkill.includes(kw)));
    return match ? match.icon : 'code-2';
  }

  private slugify(text: string): string {
    return text
      .toLowerCase()
      .normalize('NFD').replace(/[\u0300-\u036f]/g, '') // enlève les accents
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '') || 'skill';
  }

  startInterview(cat: Category): void {
    sessionStorage.setItem('interview_specialty', JSON.stringify({
      id:          cat.id,
      title:       cat.title,
      count:       cat.questions,
      duration:    cat.duration,
      description: cat.description,
      level:       cat.difficulty,
      isCustom:    !!cat.isCustom
    }));
    this.router.navigate(['/frontoffice/interview-session']);
  }

  goToCvUpload(): void { this.router.navigate(['/frontoffice/cvAnalyse']); }
}