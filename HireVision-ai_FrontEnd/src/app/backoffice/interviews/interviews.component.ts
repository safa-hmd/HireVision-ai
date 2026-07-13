import { AfterViewInit, Component, OnInit } from '@angular/core';
import { InterviewService } from '../../services/interview.service';
import { RecentInterview } from '../../services/admin-stats.service';

declare const lucide: any;
declare function showToast(message: string, type?: string): void;
declare function confirmAction(message: string, onConfirm: () => void): void;

@Component({
  selector: 'app-interviews',
  templateUrl: './interviews.component.html',
  styleUrls: ['./interviews.component.css']
})
export class InterviewsComponent implements OnInit, AfterViewInit {

  interviews: RecentInterview[] = [];
  isLoading = false;

  constructor(private interviewService: InterviewService) {}

  ngOnInit(): void {
    this.loadInterviews();
  }

  ngAfterViewInit(): void {
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  loadInterviews(): void {
    this.isLoading = true;
    this.interviewService.getAll().subscribe({
      next: (interviews) => {
        this.interviews = interviews;
        this.isLoading = false;
        setTimeout(() => lucide.createIcons(), 50);
      },
      error: () => {
        this.isLoading = false;
        showToast('Erreur lors du chargement des entretiens', 'danger');
      }
    });
  }

  scoreColor(score: number | null): string {
    if (score === null || score === undefined) return 'var(--text-muted)';
    if (score >= 70) return 'var(--success)';
    if (score >= 45) return 'var(--warning)';
    return 'var(--danger)';
  }

  deleteInterview(interview: RecentInterview): void {
    confirmAction(`Supprimer l'entretien de ${interview.candidateName} ?`, () => {
      this.interviewService.delete(interview.id).subscribe({
        next: () => {
          showToast('Entretien supprimé', 'success');
          this.loadInterviews();
        },
        error: () => showToast('Erreur lors de la suppression', 'danger')
      });
    });
  }
}
