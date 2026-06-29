import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InterviewSessionComponent } from './interview-session.component';

describe('InterviewSessionComponent', () => {
  let component: InterviewSessionComponent;
  let fixture: ComponentFixture<InterviewSessionComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [InterviewSessionComponent]
    });
    fixture = TestBed.createComponent(InterviewSessionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
