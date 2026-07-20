import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';

import { InterviewSessionComponent } from './interview-session.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../../testing/global-ui-stubs';

describe('InterviewSessionComponent', () => {
  let component: InterviewSessionComponent;
  let fixture: ComponentFixture<InterviewSessionComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    // Le composant redirige immédiatement si sessionStorage ne contient pas
    // la spécialité choisie lors de la préparation de l'entretien.
    sessionStorage.setItem('interview_specialty', JSON.stringify({
      domain: 'Backend',
      difficulty: 'MEDIUM'
    }));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      declarations: [InterviewSessionComponent]
    });
    fixture = TestBed.createComponent(InterviewSessionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    uninstallGlobalUiStubs();
    sessionStorage.removeItem('interview_specialty');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
