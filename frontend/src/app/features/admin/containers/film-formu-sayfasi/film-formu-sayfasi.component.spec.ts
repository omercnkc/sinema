import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { FilmFormuComponent } from '../../components/film-formu/film-formu.component';
import { FilmIstegi, TmdbFilmOnizlemesi, YonetimFilmDetayi } from '../../models/film-formu.models';
import { TmdbService } from '../../services/tmdb.service';
import { YonetimFilmService } from '../../services/yonetim-film.service';
import { FilmFormuSayfasiComponent } from './film-formu-sayfasi.component';

describe('FilmFormuSayfasiComponent', () => {
  const ISTEK: FilmIstegi = {
    tmdbId: 872585, title: 'Oppenheimer', overview: null, posterPath: null, runtimeMinutes: 181, releaseDate: null,
    certification: null, voteAverage: null, director: null, genres: [], cast: [], contentDescriptors: [],
  };
  const ONIZLEME: TmdbFilmOnizlemesi = { ...ISTEK, tmdbId: 872585, movieId: null };
  const KAYITLI: YonetimFilmDetayi = { ...ISTEK, id: 7, visible: true };
  let fixture: ComponentFixture<FilmFormuSayfasiComponent>;
  let filmService: jasmine.SpyObj<YonetimFilmService>;
  let tmdb: jasmine.SpyObj<TmdbService>;
  let bildirim: jasmine.SpyObj<BildirimService>;
  let yonlendir: jasmine.Spy;

  function olustur(filmId: string | null): void {
    filmService = jasmine.createSpyObj<YonetimFilmService>('YonetimFilmService', ['getir', 'olustur', 'guncelle']);
    tmdb = jasmine.createSpyObj<TmdbService>('TmdbService', ['onizleme']);
    bildirim = jasmine.createSpyObj<BildirimService>('BildirimService', ['basari']);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [FilmFormuSayfasiComponent, FilmFormuComponent],
      providers: [
        { provide: YonetimFilmService, useValue: filmService },
        { provide: TmdbService, useValue: tmdb },
        { provide: BildirimService, useValue: bildirim },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(filmId ? { id: filmId } : {}) } } },
      ],
      // TMDB arama bileşeni kendi testinde; burada seçimi doğrudan tetikliyoruz
      schemas: [NO_ERRORS_SCHEMA],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
  }

  function baslat(): void {
    fixture = TestBed.createComponent(FilmFormuSayfasiComponent);
    fixture.detectChanges();
  }

  function form(): FilmFormuComponent {
    return fixture.debugElement.query(By.directive(FilmFormuComponent)).componentInstance as FilmFormuComponent;
  }

  function gonder(): void {
    fixture.nativeElement.querySelector('app-film-formu form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('should fill form from selected TMDB film, create it and return to list', () => {
    olustur(null);
    tmdb.onizleme.and.returnValue(of(ONIZLEME));
    filmService.olustur.and.returnValue(of({ ...KAYITLI }));
    baslat();
    expect(fixture.nativeElement.querySelector('app-tmdb-arama')).not.toBeNull();

    fixture.componentInstance.tmdbSecildi(872585);
    fixture.detectChanges();
    expect(form().baslik.value).toBe('Oppenheimer');

    gonder();

    expect(filmService.olustur).toHaveBeenCalledWith(ISTEK);
    expect(bildirim.basari).toHaveBeenCalledWith('"Oppenheimer" eklendi.');
    expect(yonlendir).toHaveBeenCalledWith('/admin/filmler');
  });

  it('should not fill form with an already added TMDB film and link to it', () => {
    olustur(null);
    tmdb.onizleme.and.returnValue(of({ ...ONIZLEME, movieId: 7 }));
    baslat();

    fixture.componentInstance.tmdbSecildi(872585);
    fixture.detectChanges();

    expect(form().baslik.value).toBe('');
    expect(fixture.nativeElement.querySelector('.alert-info a').getAttribute('href')).toBe('/admin/filmler/7');
  });

  it('should load film for edit without TMDB search and update it', () => {
    olustur('7');
    filmService.getir.and.returnValue(of(KAYITLI));
    filmService.guncelle.and.returnValue(of({ ...KAYITLI, title: 'Oppenheimer (2023)' }));
    baslat();

    expect(fixture.nativeElement.querySelector('app-tmdb-arama')).toBeNull();
    form().form.patchValue({ title: 'Oppenheimer (2023)' });
    gonder();

    expect(filmService.guncelle).toHaveBeenCalledWith(7, { ...ISTEK, title: 'Oppenheimer (2023)' });
    expect(bildirim.basari).toHaveBeenCalledWith('"Oppenheimer (2023)" güncellendi.');
  });

  it('should keep form and show save error', () => {
    olustur(null);
    filmService.olustur.and.returnValue(throwError(() =>
      new UygulamaHatasi(409, 'MOVIE_ALREADY_EXISTS', 'Bu TMDB filmi zaten ekli.')));
    baslat();
    form().form.patchValue({ title: 'Oppenheimer', runtimeMinutes: 181 });

    gonder();

    expect(fixture.nativeElement.querySelector('app-film-formu [role="alert"]').textContent).toContain('zaten ekli');
    expect(yonlendir).not.toHaveBeenCalled();
    expect(form().baslik.value).toBe('Oppenheimer');
  });

  it('should show not found for invalid id without calling server', () => {
    olustur('abc');
    baslat();

    expect(filmService.getir).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Film bulunamadı.');
    expect(fixture.nativeElement.querySelector('app-film-formu')).toBeNull();
  });
});
