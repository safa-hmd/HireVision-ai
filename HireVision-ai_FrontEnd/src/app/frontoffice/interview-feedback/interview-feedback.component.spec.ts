import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';

import { InterviewFeedbackComponent } from './interview-feedback.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../../testing/global-ui-stubs';

describe('InterviewFeedbackComponent', () => {
  let component: InterviewFeedbackComponent;
  let fixture: ComponentFixture<InterviewFeedbackComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    // Le composant redirige immédiatement si sessionStorage ne contient pas
    // de résultats d'entretien : on simule un résultat minimal réaliste.
    sessionStorage.setItem('interview_results', JSON.stringify({
      specialty: { title: 'Backend' },
      answers: [],
      avg_scores: { technique: 70, communication: 65, confiance: 60 },
      behavior: {}
    }));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, TranslateModule.forRoot()],
      declarations: [InterviewFeedbackComponent]
    });
    fixture = TestBed.createComponent(InterviewFeedbackComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    uninstallGlobalUiStubs();
    sessionStorage.removeItem('interview_results');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
