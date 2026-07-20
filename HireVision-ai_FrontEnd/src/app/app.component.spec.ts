import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';

import { AppComponent } from './app.component';

describe('AppComponent', () => {
  let fixture: ComponentFixture<AppComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, TranslateModule.forRoot()],
      declarations: [AppComponent]
    });
    fixture = TestBed.createComponent(AppComponent);
  });

  it('should create the app', () => {
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it(`should have as title 'HireVision-ai_FrontEnd'`, () => {
    const app = fixture.componentInstance;
    expect(app.title).toEqual('HireVision-ai_FrontEnd');
  });

  it('should render a router-outlet', () => {
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('router-outlet')).withContext(
      'app.component.html ne contient plus le markup ".content span" du template ' +
      'par défaut Angular CLI, juste <router-outlet> : ce test vérifie donc sa présence réelle'
    ).toBeTruthy();
  });
});
