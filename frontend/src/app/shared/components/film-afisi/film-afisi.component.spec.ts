import { ComponentFixture, TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { TmdbAfisPipe } from '../../pipes/tmdb-afis.pipe';
import { FilmAfisiComponent } from './film-afisi.component';

describe('FilmAfisiComponent', () => {
  let fixture: ComponentFixture<FilmAfisiComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ declarations: [FilmAfisiComponent, TmdbAfisPipe] });
    fixture = TestBed.createComponent(FilmAfisiComponent);
    fixture.componentInstance.baslik = 'Son Tren';
  });

  it('should hotlink TMDB poster with requested size and lazy loading', () => {
    fixture.componentInstance.afisYolu = '/abc.jpg';
    fixture.componentInstance.boyut = 'w500';
    fixture.detectChanges();

    const gorsel: HTMLImageElement = fixture.nativeElement.querySelector('img');
    expect(gorsel.getAttribute('src')).toBe(`${environment.tmdbGorselTabanAdresi}/w500/abc.jpg`);
    expect(gorsel.alt).toBe('Son Tren afişi');
    expect(gorsel.getAttribute('loading')).toBe('lazy');
  });

  it('should draw placeholder with first letter when poster is missing', () => {
    fixture.detectChanges();

    const yerTutucu: HTMLElement = fixture.nativeElement.querySelector('[role="img"]');
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(yerTutucu.textContent?.trim()).toBe('S');
    expect(yerTutucu.getAttribute('aria-label')).toBe('Son Tren (afiş yok)');
  });
});
