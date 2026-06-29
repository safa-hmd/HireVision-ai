import { AfterViewInit, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

declare const lucide: any;

@Component({
  selector: 'app-interview-feedback',
  templateUrl: './interview-feedback.component.html',
  styleUrls: ['./interview-feedback.component.css']
})
export class InterviewFeedbackComponent implements OnInit, AfterViewInit {

  results: any = null;
  feedback: any = null;
  isLoading = true;
  private pythonUrl = 'http://localhost:8000';

  get globalScore(): number { return this.results?.avg_scores?.global || 0; }
  get scoreColor(): string {
    if (this.globalScore >= 75) return '#10B981';
    if (this.globalScore >= 50) return '#F59E0B';
    return '#EF4444';
  }

  constructor(private router: Router, private http: HttpClient) {}

  ngOnInit(): void {
    const stored = sessionStorage.getItem('interview_results');
    if (!stored) { this.router.navigate(['/frontoffice/interviewPrep']); return; }
    this.results = JSON.parse(stored);
    this.loadFeedback();
  }

  ngAfterViewInit(): void { setTimeout(() => lucide.createIcons(), 100); }

  loadFeedback(): void {
    this.http.post<any>(`${this.pythonUrl}/interview/feedback`, {
      specialty:  this.results.specialty.title,
      answers:    this.results.answers.map((a: any) => ({
        question:   a.question?.question || '',
        transcript: a.transcript || '',
        score:      a.evaluation?.score_global || 0
      })),
      avg_scores: this.results.avg_scores
    }).subscribe({
      next: (fb) => {
        this.feedback  = fb;
        this.isLoading = false;
        setTimeout(() => lucide.createIcons(), 100);
      },
      error: () => {
        this.feedback = {
          titre: this.globalScore >= 75 ? 'Excellent travail ! 🎉' : 'Bon effort !',
          message_global: 'Votre entretien a été analysé avec succès.',
          points_forts: ['Bonne participation', 'Réponses cohérentes', 'Communication correcte'],
          axes_amelioration: ['Approfondissez les concepts avancés', 'Donnez plus d\'exemples concrets'],
          conseil_final: 'Continuez à pratiquer régulièrement !',
          recommandation: this.globalScore >= 60 ? 'En bonne voie' : 'Préparation supplémentaire recommandée'
        };
        this.isLoading = false;
        setTimeout(() => lucide.createIcons(), 100);
      }
    });
  }

  formatDuration(s: number): string {
    return `${Math.floor(s / 60)}m ${s % 60}s`;
  }

  getNiveauColor(n: string): string {
    if (n === 'Excellent') return '#10B981';
    if (n === 'Bien')      return '#3B82F6';
    if (n === 'Moyen')     return '#F59E0B';
    return '#EF4444';
  }

  restart(): void {
    sessionStorage.removeItem('interview_results');
    this.router.navigate(['/frontoffice/interviewPrep']);
  }
}