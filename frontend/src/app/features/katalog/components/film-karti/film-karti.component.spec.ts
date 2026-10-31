import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { SharedModule } from '../../../../shared/shared.module';
import { FilmKartiComponent } from './film-karti.component';

describe('FilmKartiComponent', () => {
  beforeAll(() => registerLocaleData(localeTr));

  it('should show title link, certification, runtime, rating and genres', () => {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [FilmKartiComponent],
      providers: [{ provide: LOCALE_ID, useValue: 'tr' }],
    });
    const fixture = TestBed.createComponent(FilmKartiComponent);
    fixture.componentInstance.film = {
      id: 5, title: 'Son Tren', posterPath: null, runtimeMinutes: 124, releaseDate: '2026-10-01',
      certification: '18+', voteAverage: 7.7, genres: ['Dram', 'Gerilim'],
    };
    fixture.detectChanges();
    const kok: HTMLElement = fixture.nativeElement;

    expect(kok.querySelector('a')?.getAttribute('href')).toBe('/filmler/5');
    expect(kok.querySelector('.badge')?.textContent).toBe('18+');
    expect(kok.textContent).toContain('124 dk');
    expect(kok.textContent).toContain('7,7');
    expect(kok.textContent).toContain('Dram, Gerilim');
  });
});
