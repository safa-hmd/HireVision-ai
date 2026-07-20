import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';

import { QuestionsComponent } from './questions.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../../testing/global-ui-stubs';

describe('QuestionsComponent', () => {
  let component: QuestionsComponent;
  let fixture: ComponentFixture<QuestionsComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [QuestionsComponent]
    });
    fixture = TestBed.createComponent(QuestionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => uninstallGlobalUiStubs());

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
