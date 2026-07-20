import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';

import { BackofficeComponent } from './backoffice.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../testing/global-ui-stubs';

describe('BackofficeComponent', () => {
  let component: BackofficeComponent;
  let fixture: ComponentFixture<BackofficeComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [BackofficeComponent],
      // <app-menu> est un vrai composant avec sa propre arborescence de
      // dépendances : on ne le monte pas ici, ce test ne couvre que le
      // shell (layout + hooks lucide/charts) de BackofficeComponent.
      schemas: [NO_ERRORS_SCHEMA]
    });
    fixture = TestBed.createComponent(BackofficeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => uninstallGlobalUiStubs());

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
