import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NgbCollapseModule } from '@ng-bootstrap/ng-bootstrap';
import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  let fixture: ComponentFixture<HeaderComponent>;
  let kok: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, NgbCollapseModule],
      declarations: [HeaderComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(HeaderComponent);
    kok = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('should link brand to home and show films menu', () => {
    expect(kok.querySelector('a.navbar-brand')?.getAttribute('href')).toBe('/');
    expect(kok.querySelector('a.nav-link')?.textContent?.trim()).toBe('Filmler');
  });

  it('should toggle menu and reflect state in aria-expanded', () => {
    const dugme = kok.querySelector<HTMLButtonElement>('button.navbar-toggler');
    expect(dugme?.getAttribute('aria-expanded')).toBe('false');

    dugme?.click();
    fixture.detectChanges();

    expect(dugme?.getAttribute('aria-expanded')).toBe('true');
  });
});
