import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { Sayfa } from '../../../../shared/models/sayfa.models';
import { SharedModule } from '../../../../shared/shared.module';
import { YonetimFilmOzeti } from '../../models/yonetim-film.models';
import { YonetimFilmService } from '../../services/yonetim-film.service';
import { FilmYonetimiSayfasiComponent } from './film-yonetimi-sayfasi.component';

describe('FilmYonetimiSayfasiComponent', () => {
  const GORUNUR: YonetimFilmOzeti = {
    id: 1, title: 'Son Tren', posterPath: null, runtimeMinutes: 124, releaseDate: '2026-10-01', certification: '13+',
    voteAverage: null, genres: [], visible: true,
  };
  const GIZLI: YonetimFilmOzeti = { ...GORUNUR, id: 2, title: 'Eski Film', visible: false };
  const SAYFA: Sayfa<YonetimFilmOzeti> = { content: [GORUNUR, GIZLI], page: 0, size: 20, totalElements: 2, totalPages: 1 };
  let fixture: ComponentFixture<FilmYonetimiSayfasiComponent>;
  let filmService: jasmine.SpyObj<YonetimFilmService>;
  let bildirim: jasmine.SpyObj<BildirimService>;
  let parametreler: BehaviorSubject<ParamMap>;
  let yonlendir: jasmine.Spy;

  beforeEach(() => {
    parametreler = new BehaviorSubject(convertToParamMap({}));
    filmService = jasmine.createSpyObj<YonetimFilmService>('YonetimFilmService', ['ara', 'gorunurlukDegistir']);
    filmService.ara.and.returnValue(of(SAYFA));
    bildirim = jasmine.createSpyObj<BildirimService>('BildirimService', ['basari', 'hata']);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [FilmYonetimiSayfasiComponent],
      providers: [
        { provide: YonetimFilmService, useValue: filmService },
        { provide: BildirimService, useValue: bildirim },
        { provide: ActivatedRoute, useValue: { queryParamMap: parametreler } },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(FilmYonetimiSayfasiComponent);
  });

  function satirlar(): HTMLElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('[data-test="film-satiri"]'));
  }

  function dugme(satir: number): HTMLButtonElement {
    return satirlar()[satir].querySelector('button') as HTMLButtonElement;
  }

  it('should list hidden and visible films from query parameters', () => {
    parametreler.next(convertToParamMap({ ara: 'tren', sayfa: '2' }));
    fixture.detectChanges();

    expect(filmService.ara).toHaveBeenCalledWith({ arama: 'tren', sayfa: 1 });
    expect(satirlar().map((satir) => satir.querySelector('.badge')?.textContent?.trim())).toEqual(['Görünür', 'Gizli']);
    expect(satirlar()[0].textContent).toContain('2026 · 124 dk · 13+');
    expect(dugme(0).textContent?.trim()).toBe('Gizle');
    expect(dugme(1).textContent?.trim()).toBe('Göster');
  });

  it('should write search to address after typing pauses', fakeAsync(() => {
    fixture.detectChanges();

    fixture.componentInstance.aramaKontrolu.setValue(' tren ');
    tick(300);

    expect((yonlendir.calls.mostRecent().args[1] as { queryParams: unknown }).queryParams)
      .toEqual({ ara: 'tren', sayfa: null });
  }));

  it('should toggle visibility, disable button meanwhile and refresh list silently', () => {
    const cevap$ = new Subject<unknown>();
    filmService.gorunurlukDegistir.and.returnValue(cevap$);
    fixture.detectChanges();

    dugme(0).click();
    dugme(0).click();
    fixture.detectChanges();

    expect(filmService.gorunurlukDegistir).toHaveBeenCalledOnceWith(1, false);
    expect(dugme(0).disabled).toBeTrue();
    expect(dugme(1).disabled).toBeFalse();

    filmService.ara.and.returnValue(of({ ...SAYFA, content: [{ ...GORUNUR, visible: false }, GIZLI] }));
    cevap$.next({});
    cevap$.complete();
    fixture.detectChanges();

    expect(bildirim.basari).toHaveBeenCalledWith('"Son Tren" gizlendi. Mevcut seanslar ve satılmış biletler etkilenmez.');
    expect(filmService.ara).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.querySelector('app-yukleniyor')).toBeNull();
    expect(dugme(0).disabled).toBeFalse();
    expect(dugme(0).textContent?.trim()).toBe('Göster');
  });

  it('should notify toggle error and enable button again', () => {
    filmService.gorunurlukDegistir.and.returnValue(throwError(() =>
      new UygulamaHatasi(404, 'MOVIE_NOT_FOUND', 'Film bulunamadı.')));
    fixture.detectChanges();

    dugme(1).click();
    fixture.detectChanges();

    expect(filmService.gorunurlukDegistir).toHaveBeenCalledWith(2, true);
    expect(bildirim.hata).toHaveBeenCalledWith('Film bulunamadı.');
    expect(dugme(1).disabled).toBeFalse();
  });

  it('should show error with retry', () => {
    filmService.ara.and.returnValue(throwError(() => new UygulamaHatasi(503, 'CATALOG_UNAVAILABLE', 'Katalog kullanılamıyor.')));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-hata-mesaji')).not.toBeNull();

    filmService.ara.and.returnValue(of(SAYFA));
    fixture.componentInstance.tekrarDene();
    fixture.detectChanges();

    expect(satirlar().length).toBe(2);
  });
});
