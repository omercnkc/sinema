import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { Sayfa } from '../../../../shared/models/sayfa.models';
import { SharedModule } from '../../../../shared/shared.module';
import { FilmOzeti } from '../../../../shared/models/film.models';
import { FilmService } from '../../services/film.service';
import { FilmListesiSayfasiComponent } from './film-listesi-sayfasi.component';

describe('FilmListesiSayfasiComponent', () => {
  const FILM: FilmOzeti = {
    id: 1, title: 'Son Tren', posterPath: null, runtimeMinutes: 124, releaseDate: null, certification: null,
    voteAverage: null, genres: ['Dram'],
  };
  const SAYFA: Sayfa<FilmOzeti> = { content: [FILM], page: 0, size: 12, totalElements: 1, totalPages: 1 };
  let fixture: ComponentFixture<FilmListesiSayfasiComponent>;
  let filmService: jasmine.SpyObj<FilmService>;
  let parametreler: BehaviorSubject<ParamMap>;
  let yonlendir: jasmine.Spy;

  beforeEach(() => {
    parametreler = new BehaviorSubject(convertToParamMap({}));
    filmService = jasmine.createSpyObj<FilmService>('FilmService', ['ara', 'turler']);
    filmService.ara.and.returnValue(of(SAYFA));
    filmService.turler.and.returnValue(of(['Dram', 'Komedi']));
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [FilmListesiSayfasiComponent],
      providers: [
        { provide: FilmService, useValue: filmService },
        { provide: ActivatedRoute, useValue: { queryParamMap: parametreler } },
      ],
      // Film kartı ve ngb-pagination bu testin konusu değil
      schemas: [NO_ERRORS_SCHEMA],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(FilmListesiSayfasiComponent);
  });

  function sonYonlendirmeParametreleri(): unknown {
    return (yonlendir.calls.mostRecent().args[1] as { queryParams: unknown }).queryParams;
  }

  it('should load list from query parameters (page is 1-based in the address)', () => {
    parametreler.next(convertToParamMap({ ara: 'tren', tur: 'Dram', sayfa: '3' }));
    fixture.detectChanges();

    expect(filmService.ara).toHaveBeenCalledWith({ arama: 'tren', tur: 'Dram', sayfa: 2 });
    expect(fixture.nativeElement.querySelectorAll('app-film-karti').length).toBe(1);
    expect((fixture.nativeElement.querySelector('#film-arama') as HTMLInputElement).value).toBe('tren');
  });

  it('should write search to address only after typing pauses and reset page', fakeAsync(() => {
    fixture.detectChanges();
    const kutu = fixture.componentInstance.aramaKontrolu;

    kutu.setValue('t');
    tick(100);
    kutu.setValue('tren ');
    tick(299);
    expect(yonlendir).not.toHaveBeenCalled();

    tick(1);
    expect(yonlendir).toHaveBeenCalledTimes(1);
    expect(sonYonlendirmeParametreleri()).toEqual({ ara: 'tren', sayfa: null });
  }));

  it('should write genre and page changes to address', () => {
    fixture.detectChanges();

    fixture.componentInstance.turSecildi('Komedi');
    expect(sonYonlendirmeParametreleri()).toEqual({ tur: 'Komedi', sayfa: null });

    fixture.componentInstance.sayfaDegisti(2);
    expect(sonYonlendirmeParametreleri()).toEqual({ sayfa: 2 });

    fixture.componentInstance.turSecildi('');
    expect(sonYonlendirmeParametreleri()).toEqual({ tur: null, sayfa: null });
  });

  it('should show empty result message', () => {
    filmService.ara.and.returnValue(of({ ...SAYFA, content: [], totalElements: 0, totalPages: 0 }));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aramanıza uygun film bulunamadı.');
  });

  it('should show error with retry that reloads the list', () => {
    filmService.ara.and.returnValue(throwError(() => new UygulamaHatasi(503, 'CATALOG_UNAVAILABLE', 'Katalog kullanılamıyor.')));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-hata-mesaji')).not.toBeNull();

    filmService.ara.and.returnValue(of(SAYFA));
    fixture.componentInstance.tekrarDene();
    fixture.detectChanges();

    expect(filmService.ara).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('app-hata-mesaji')).toBeNull();
  });
});
