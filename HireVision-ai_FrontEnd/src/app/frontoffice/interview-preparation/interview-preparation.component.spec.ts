import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InterviewPreparationComponent } from './interview-preparation.component';

describe('InterviewPreparationComponent', () => {
  let component: InterviewPreparationComponent;
  let fixture: ComponentFixture<InterviewPreparationComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [InterviewPreparationComponent]
    });
    fixture = TestBed.createComponent(InterviewPreparationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
