import { AfterViewInit, Component, OnInit } from '@angular/core';
import { QuestionService, QuestionDTO } from '../../services/question.service';

declare const lucide: any;
declare function showToast(message: string, type?: string): void;
declare function confirmAction(message: string, onConfirm: () => void): void;

@Component({
  selector: 'app-questions',
  templateUrl: './questions.component.html',
  styleUrls: ['./questions.component.css']
})
export class QuestionsComponent implements OnInit, AfterViewInit {

  allQuestions: QuestionDTO[] = [];
  filteredQuestions: QuestionDTO[] = [];
  isLoading = false;
  difficultyFilter: '' | 'EASY' | 'MEDIUM' | 'HARD' = '';
  searchText = '';

  // Pagination
  currentPage = 1;
  pageSize = 10;

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.filteredQuestions.length / this.pageSize));
  }

  constructor(private questionService: QuestionService) {}

  ngOnInit(): void {
    this.loadQuestions();
  }

  ngAfterViewInit(): void {
    if (typeof lucide !== 'undefined') lucide.createIcons();
  }

  loadQuestions(): void {
    this.isLoading = true;
    this.questionService.getAll().subscribe({
      next: (questions) => {
        this.allQuestions = questions;
        this.applyFilters();
        this.isLoading = false;
        setTimeout(() => lucide.createIcons(), 50);
      },
      error: () => {
        this.isLoading = false;
        showToast('Erreur lors du chargement des questions', 'danger');
      }
    });
  }

  applyFilters(): void {
    this.filteredQuestions = this.allQuestions.filter(q => {
      const matchesDifficulty = !this.difficultyFilter || q.difficulty === this.difficultyFilter;
      const matchesSearch = !this.searchText.trim()
        || q.content.toLowerCase().includes(this.searchText.trim().toLowerCase());
      return matchesDifficulty && matchesSearch;
    });
    this.currentPage = 1;
  }

  difficultyLabel(difficulty: string): string {
    switch (difficulty) {
      case 'EASY': return 'Facile';
      case 'MEDIUM': return 'Moyen';
      case 'HARD': return 'Difficile';
      default: return difficulty;
    }
  }

  deleteQuestion(question: QuestionDTO): void {
    confirmAction('Supprimer cette question ?', () => {
      this.questionService.delete(question.id).subscribe({
        next: () => {
          showToast('Question supprimée', 'success');
          this.loadQuestions();
        },
        error: () => showToast('Erreur lors de la suppression', 'danger')
      });
    });
  }
}
