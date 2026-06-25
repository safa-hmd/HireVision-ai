/// <reference types="jasmine" />
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FrontofficeComponent } from './frontoffice.component';

describe('FrontofficeComponent', () => {
  let component: FrontofficeComponent;
  let fixture: ComponentFixture<FrontofficeComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [FrontofficeComponent]
    });
    fixture = TestBed.createComponent(FrontofficeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
