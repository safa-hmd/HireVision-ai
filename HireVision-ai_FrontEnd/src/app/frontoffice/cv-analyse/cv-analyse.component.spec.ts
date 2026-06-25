import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CvAnalyseComponent } from './cv-analyse.component';

describe('CvAnalyseComponent', () => {
  let component: CvAnalyseComponent;
  let fixture: ComponentFixture<CvAnalyseComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [CvAnalyseComponent]
    });
    fixture = TestBed.createComponent(CvAnalyseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
