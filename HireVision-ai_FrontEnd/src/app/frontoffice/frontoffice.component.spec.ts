import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';

import { FrontofficeComponent } from './frontoffice.component';

describe('FrontofficeComponent', () => {
  let component: FrontofficeComponent;
  let fixture: ComponentFixture<FrontofficeComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [FrontofficeComponent],
      // <app-menu> a sa propre arborescence de dépendances (services HTTP,
      // LanguageService...) : hors du périmètre de ce test de shell.
      schemas: [NO_ERRORS_SCHEMA]
    });
    fixture = TestBed.createComponent(FrontofficeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
