import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InterviewsComponent } from './interviews.component';

describe('InterviewsComponent', () => {
  let component: InterviewsComponent;
  let fixture: ComponentFixture<InterviewsComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [InterviewsComponent]
    });
    fixture = TestBed.createComponent(InterviewsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
