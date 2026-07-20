import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TranslateModule } from '@ngx-translate/core';

import { CvAnalyseComponent } from './cv-analyse.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../../testing/global-ui-stubs';

describe('CvAnalyseComponent', () => {
  let component: CvAnalyseComponent;
  let fixture: ComponentFixture<CvAnalyseComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    // Sans userId, loadExistingAnalysis() positionne isChecking = false de
    // façon synchrone dans ngAfterViewInit, ce qui déclenche
    // ExpressionChangedAfterItHasBeenCheckedError (NG0100) juste après le
    // premier detectChanges(). Avec un userId, ce même flag n'est modifié
    // qu'au retour (asynchrone, jamais flush ici) de l'appel HTTP.
    localStorage.setItem('UserIdConnect', '1');

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, TranslateModule.forRoot()],
      declarations: [CvAnalyseComponent]
    });
    fixture = TestBed.createComponent(CvAnalyseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    uninstallGlobalUiStubs();
    localStorage.removeItem('UserIdConnect');
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
