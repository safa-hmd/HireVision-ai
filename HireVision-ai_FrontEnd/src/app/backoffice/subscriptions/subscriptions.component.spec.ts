import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';

import { SubscriptionsComponent } from './subscriptions.component';
import { installGlobalUiStubs, uninstallGlobalUiStubs } from '../../testing/global-ui-stubs';

describe('SubscriptionsComponent', () => {
  let component: SubscriptionsComponent;
  let fixture: ComponentFixture<SubscriptionsComponent>;

  beforeEach(() => {
    installGlobalUiStubs();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [SubscriptionsComponent]
    });
    fixture = TestBed.createComponent(SubscriptionsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => uninstallGlobalUiStubs());

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
