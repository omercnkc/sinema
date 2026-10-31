import { TestBed } from '@angular/core/testing';
import { FooterComponent } from './footer.component';

describe('FooterComponent', () => {
  it('should show TMDB logo and required attribution text', () => {
    TestBed.configureTestingModule({ declarations: [FooterComponent] });
    const fixture = TestBed.createComponent(FooterComponent);
    fixture.detectChanges();
    const kok: HTMLElement = fixture.nativeElement;

    const logo = kok.querySelector('img');
    expect(logo?.getAttribute('src')).toBe('assets/tmdb/tmdb-logo-kisa.svg');
    expect(logo?.getAttribute('alt')).toContain('TMDB');
    expect(kok.textContent).toContain('This product uses the TMDB API but is not endorsed or certified by TMDB.');
  });
});
