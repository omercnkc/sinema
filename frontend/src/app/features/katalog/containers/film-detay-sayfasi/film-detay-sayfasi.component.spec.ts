import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { FilmDetayi } from '../../models/film.models';
import { FilmService } from '../../services/film.service';
import { FilmDetaySayfasiComponent } from './film-detay-sayfasi.component';

describe('FilmDetaySayfasiComponent', () => {
  const FILM: FilmDetayi = {
    id: 3, tmdbId: 872585, title: 'Oppenheimer', posterPath: '/a.jpg', runtimeMinutes: 181, releaseDate: '2023-07-21',
    certification: '13+', voteAverage: 8.1, genres: ['Dram', 'Tarih'], overview: 'Birinci paragraf.\nİkinci paragraf.',
    director: 'Christopher Nolan', cast: ['Cillian Murphy', 'Emily Blunt'], contentDescriptors: ['CINSELLIK', 'OLUMSUZ_ORNEK'],
  };
  let filmService: jasmine.SpyObj<FilmService>;

  function olustur(id: string): ComponentFixture<FilmDetaySayfasiComponent> {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [FilmDetaySayfasiComponent],
      providers: [
        { provide: FilmService, useValue: filmService },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } },
      ],
      // Seans seçimi (app-film-seanslari) kendi testinde
      schemas: [NO_ERRORS_SCHEMA],
    });
    const fixture = TestBed.createComponent(FilmDetaySayfasiComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(() => {
    filmService = jasmine.createSpyObj<FilmService>('FilmService', ['getir']);
  });

  it('should show all movie fields with TMDB rating label and content descriptors', () => {
    filmService.getir.and.returnValue(of(FILM));
    const kok: HTMLElement = olustur('3').nativeElement;

    expect(filmService.getir).toHaveBeenCalledWith(3);
    expect(kok.querySelector('h1')?.textContent).toBe('Oppenheimer');
    // Etiket ile yıldız arasındaki boşluk (&ngsp;) şablon derlemesinde silinmemeli
    expect(kok.textContent).toContain('TMDB puanı: ★');
    expect(kok.textContent).toContain('Christopher Nolan');
    expect(kok.textContent).toContain('Cillian Murphy, Emily Blunt');
    const isaretler = Array.from(kok.querySelectorAll('[aria-label="Akıllı işaretler"] .badge')).map((e) => e.textContent);
    expect(isaretler).toEqual(['Cinsellik', 'Olumsuz örnek oluşturabilecek davranışlar']);
  });

  it('should label rating without TMDB for manually entered movies', () => {
    filmService.getir.and.returnValue(of({ ...FILM, tmdbId: null }));

    expect(olustur('3').nativeElement.textContent).toContain('Puan:');
  });

  it('should show not found without retry and without calling server for invalid id', () => {
    const kok: HTMLElement = olustur('abc').nativeElement;

    expect(filmService.getir).not.toHaveBeenCalled();
    expect(kok.textContent).toContain('Film bulunamadı.');
    expect(kok.querySelector('app-hata-mesaji button')).toBeNull();
  });

  it('should offer retry when service is unavailable', () => {
    filmService.getir.and.returnValue(throwError(() => new UygulamaHatasi(503, 'CATALOG_UNAVAILABLE', 'Katalog kullanılamıyor.')));

    expect(olustur('3').nativeElement.querySelector('app-hata-mesaji button')).not.toBeNull();
  });
});
