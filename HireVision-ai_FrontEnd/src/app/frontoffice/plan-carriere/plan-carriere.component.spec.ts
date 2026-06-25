import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PlanCarriereComponent } from './plan-carriere.component';

describe('PlanCarriereComponent', () => {
  let component: PlanCarriereComponent;
  let fixture: ComponentFixture<PlanCarriereComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PlanCarriereComponent]
    });
    fixture = TestBed.createComponent(PlanCarriereComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
