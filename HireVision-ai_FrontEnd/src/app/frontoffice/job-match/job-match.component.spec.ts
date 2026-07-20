import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';

import { JobMatchComponent } from './job-match.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../../testing/global-ui-stubs';

describe('JobMatchComponent', () => {
  let component: JobMatchComponent;
  let fixture: ComponentFixture<JobMatchComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule, TranslateModule.forRoot()],
      declarations: [JobMatchComponent]
    });
    fixture = TestBed.createComponent(JobMatchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => uninstallGlobalUiStubs());

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
