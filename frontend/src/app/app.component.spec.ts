import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { LayoutModule } from './layout/layout.module';

describe('AppComponent', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, LayoutModule],
      declarations: [AppComponent],
    }).compileComponents();
  });

  it('should render header, main content, footer and notification area', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const kok: HTMLElement = fixture.nativeElement;

    expect(kok.querySelector('app-header')).not.toBeNull();
    expect(kok.querySelector('main router-outlet')).not.toBeNull();
    expect(kok.querySelector('app-footer')).not.toBeNull();
    expect(kok.querySelector('app-bildirim-alani')).not.toBeNull();
  });
});
