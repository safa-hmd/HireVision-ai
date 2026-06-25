import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobMatchComponent } from './job-match.component';

describe('JobMatchComponent', () => {
  let component: JobMatchComponent;
  let fixture: ComponentFixture<JobMatchComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [JobMatchComponent]
    });
    fixture = TestBed.createComponent(JobMatchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
