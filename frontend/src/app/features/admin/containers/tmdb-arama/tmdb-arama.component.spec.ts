import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { TmdbAramaSonucu } from '../../models/film-formu.models';
import { TmdbService } from '../../services/tmdb.service';
import { TmdbAramaComponent } from './tmdb-arama.component';

describe('TmdbAramaComponent', () => {
  const SONUC: TmdbAramaSonucu = {
    page: 1, totalPages: 1, totalResults: 2, results: [
      { tmdbId: 872585, title: 'Oppenheimer', originalTitle: 'Oppenheimer', releaseDate: '2023-07-21', posterPath: '/a.jpg', movieId: null },
      { tmdbId: 27205, title: 'Başlangıç', originalTitle: 'Inception', releaseDate: '2010-07-15', posterPath: null, movieId: 4 },
    ],
  };
  let fixture: ComponentFixture<TmdbAramaComponent>;
  let tmdb: jasmine.SpyObj<TmdbService>;
  let secilen: number | null;

  beforeEach(() => {
    tmdb = jasmine.createSpyObj<TmdbService>('TmdbService', ['ara']);
    tmdb.ara.and.returnValue(of(SONUC));
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [TmdbAramaComponent],
      providers: [{ provide: TmdbService, useValue: tmdb }],
    });
    fixture = TestBed.createComponent(TmdbAramaComponent);
    secilen = null;
    fixture.componentInstance.secildi.subscribe((tmdbId: number) => (secilen = tmdbId));
    fixture.detectChanges();
  });

  function ara(sorgu: string): void {
    fixture.componentInstance.aramaKontrolu.setValue(sorgu);
    tick(400);
    fixture.detectChanges();
  }

  function adaylar(): HTMLElement[] {
    return Array.from(fixture.nativeElement.querySelectorAll('[data-test="tmdb-adayi"]'));
  }

  it('should search after typing pauses and ignore short queries', fakeAsync(() => {
    ara('o');
    expect(tmdb.ara).not.toHaveBeenCalled();

    fixture.componentInstance.aramaKontrolu.setValue('oppen');
    tick(200);
    ara(' oppenheimer ');

    expect(tmdb.ara).toHaveBeenCalledOnceWith('oppenheimer');
    expect(adaylar().length).toBe(2);
    expect(adaylar()[1].textContent).toContain('Inception');
  }));

  it('should emit TMDB id on select and offer edit link for already added film', fakeAsync(() => {
    ara('film');

    (adaylar()[0].querySelector('button') as HTMLButtonElement).click();
    expect(secilen).toBe(872585);

    expect(adaylar()[1].querySelector('button')).toBeNull();
    expect(adaylar()[1].querySelector('a')?.getAttribute('href')).toBe('/admin/filmler/4');
  }));

  it('should show TMDB error and point to manual entry', fakeAsync(() => {
    tmdb.ara.and.returnValue(throwError(() => new UygulamaHatasi(503, 'TMDB_UNAVAILABLE', 'TMDB şu an kullanılamıyor.')));

    ara('film');

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('elle girebilirsiniz');
  }));
});
