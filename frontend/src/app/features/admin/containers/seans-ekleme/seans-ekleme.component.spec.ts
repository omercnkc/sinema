import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { YonetimFilmOzeti } from '../../models/yonetim-film.models';
import { SeansYonetimiService } from '../../services/seans-yonetimi.service';
import { YonetimFilmService } from '../../services/yonetim-film.service';
import { SeansEklemeComponent } from './seans-ekleme.component';

describe('SeansEklemeComponent', () => {
  const FILM: YonetimFilmOzeti = {
    id: 3, title: 'Son Tren', posterPath: null, runtimeMinutes: 124, releaseDate: null, certification: null,
    voteAverage: null, genres: [], visible: true,
  };
  const GIZLI: YonetimFilmOzeti = { ...FILM, id: 4, title: 'Son Tren 2', visible: false };
  let fixture: ComponentFixture<SeansEklemeComponent>;
  let seansService: jasmine.SpyObj<SeansYonetimiService>;
  let filmService: jasmine.SpyObj<YonetimFilmService>;
  let eklenme: number;

  beforeEach(() => {
    seansService = jasmine.createSpyObj<SeansYonetimiService>('SeansYonetimiService', ['olustur']);
    filmService = jasmine.createSpyObj<YonetimFilmService>('YonetimFilmService', ['ara']);
    filmService.ara.and.returnValue(of({ content: [FILM, GIZLI], page: 0, size: 20, totalElements: 2, totalPages: 1 }));
    TestBed.configureTestingModule({
      imports: [SharedModule],
      declarations: [SeansEklemeComponent],
      providers: [
        { provide: SeansYonetimiService, useValue: seansService },
        { provide: YonetimFilmService, useValue: filmService },
      ],
    });
    fixture = TestBed.createComponent(SeansEklemeComponent);
    fixture.componentInstance.salon = { id: 11, cinemaId: 1, name: 'Salon 1', defaultTicketPrice: '160.00', activeLayoutId: 5 };
    fixture.componentInstance.gun = '2026-11-02';
    eklenme = 0;
    fixture.componentInstance.eklendi.subscribe(() => eklenme++);
    fixture.detectChanges();
  });

  function kok(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function yaz(secici: string, deger: string): void {
    const kutu = kok().querySelector(secici) as HTMLInputElement;
    kutu.value = deger;
    kutu.dispatchEvent(new Event('input'));
  }

  function gonder(): void {
    kok().querySelector('form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function filmiSec(): void {
    yaz('#seans-film-arama', 'son tren');
    tick(300);
    fixture.detectChanges();
    (kok().querySelector('[data-test="film-adayi"]') as HTMLButtonElement).click();
    fixture.detectChanges();
  }

  it('should suggest only visible films and require film and time', fakeAsync(() => {
    gonder();
    expect(seansService.olustur).not.toHaveBeenCalled();
    expect(kok().querySelector('#seans-film-hata')?.textContent?.trim()).toBe('Film zorunludur.');
    expect(kok().querySelector('#seans-saat-hata')?.textContent?.trim()).toBe('Başlangıç saati zorunludur.');

    yaz('#seans-film-arama', 'son tren');
    tick(300);
    fixture.detectChanges();

    expect(filmService.ara).toHaveBeenCalledWith({ arama: 'son tren', sayfa: 0 });
    expect(Array.from(kok().querySelectorAll('[data-test="film-adayi"]')).map((a) => a.textContent?.trim()))
      .toEqual(['Son Tren · 124 dk']);
  }));

  it('should send Istanbul time as UTC with empty price and offset as defaults, keep film after success', fakeAsync(() => {
    seansService.olustur.and.returnValue(of({}));
    filmiSec();
    yaz('#seans-saat', '20:15');

    gonder();

    expect(seansService.olustur).toHaveBeenCalledWith({
      movieId: 3, hallId: 11, startsAt: '2026-11-02T17:15:00.000Z', ticketPrice: null, salesCloseOffsetMinutes: null,
    });
    expect(eklenme).toBe(1);
    expect(kok().querySelector('[data-test="secilen-film"]')?.textContent).toContain('Son Tren');
    expect((kok().querySelector('#seans-saat') as HTMLInputElement).value).toBe('');
  }));

  it('should send custom price and negative offset, show overlap under time field', fakeAsync(() => {
    seansService.olustur.and.returnValue(throwError(() => new UygulamaHatasi(409, 'SHOWTIME_OVERLAP',
      'Bu salonda aynı zaman aralığında başka bir seans var (seans arası payı dahil).')));
    filmiSec();
    yaz('#seans-saat', '21:00');
    yaz('#seans-fiyat', '175,5');
    yaz('#seans-ofset', '-15');

    gonder();

    expect(seansService.olustur).toHaveBeenCalledWith(jasmine.objectContaining({ ticketPrice: '175.5', salesCloseOffsetMinutes: -15 }));
    expect(kok().querySelector('#seans-saat-hata')?.textContent).toContain('başka bir seans var');
    expect(eklenme).toBe(0);
  }));

  it('should reject offset out of range on client', fakeAsync(() => {
    filmiSec();
    yaz('#seans-saat', '21:00');
    yaz('#seans-ofset', '121');

    gonder();

    expect(seansService.olustur).not.toHaveBeenCalled();
    expect(kok().querySelector('#seans-ofset-hata')?.textContent?.trim()).toBe('Satış kapanışı en çok 120 olabilir.');
  }));
});
