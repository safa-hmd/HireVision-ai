import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslateModule } from '@ngx-translate/core';

import { PlanCarriereComponent } from './plan-carriere.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../../testing/global-ui-stubs';

describe('PlanCarriereComponent', () => {
  let component: PlanCarriereComponent;
  let fixture: ComponentFixture<PlanCarriereComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, TranslateModule.forRoot()],
      declarations: [PlanCarriereComponent]
    });
    fixture = TestBed.createComponent(PlanCarriereComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => uninstallGlobalUiStubs());

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
