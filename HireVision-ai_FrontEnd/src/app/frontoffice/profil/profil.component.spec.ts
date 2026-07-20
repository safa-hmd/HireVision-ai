import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { ProfilComponent } from './profil.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../../testing/global-ui-stubs';

describe('ProfilComponent', () => {
  let component: ProfilComponent;
  let fixture: ComponentFixture<ProfilComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule, TranslateModule.forRoot()],
      declarations: [ProfilComponent]
    });
    fixture = TestBed.createComponent(ProfilComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => uninstallGlobalUiStubs());

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
