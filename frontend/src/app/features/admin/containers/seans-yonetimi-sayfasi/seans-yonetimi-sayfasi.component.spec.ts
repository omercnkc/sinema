import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { LOCALE_ID, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { istanbulGunu } from '../../../../core/zaman/is-gunu';
import { SharedModule } from '../../../../shared/shared.module';
import { SeansDuzenlemeFormuComponent } from '../../components/seans-duzenleme-formu/seans-duzenleme-formu.component';
import { YonetimSeansOzeti } from '../../models/seans-yonetimi.models';
import { YonetimSube } from '../../models/sube-yonetimi.models';
import { SeansYonetimiService } from '../../services/seans-yonetimi.service';
import { SubeYonetimiService } from '../../services/sube-yonetimi.service';
import { SeansYonetimiSayfasiComponent } from './seans-yonetimi-sayfasi.component';

describe('SeansYonetimiSayfasiComponent', () => {
  const SUBELER: YonetimSube[] = [
    { id: 1, name: 'Kadıköy', city: 'İstanbul', address: 'A', halls: [
      { id: 11, cinemaId: 1, name: 'Salon 1', defaultTicketPrice: '160.00', activeLayoutId: 5 },
      { id: 12, cinemaId: 1, name: 'Salon 2', defaultTicketPrice: '150.00', activeLayoutId: null },
    ] },
    { id: 2, name: 'Beşiktaş', city: 'İstanbul', address: 'B', halls: [
      { id: 21, cinemaId: 2, name: 'Salon A', defaultTicketPrice: '170.00', activeLayoutId: 8 },
    ] },
  ];
  const SEANS: YonetimSeansOzeti = {
    id: 100, movieId: 3, movieTitle: 'Son Tren', movieVisible: true, startsAt: '2026-11-02T17:00:00Z',
    endsAt: '2026-11-02T19:19:00Z', ticketPrice: '160.00', salesCloseOffsetMinutes: 10,
    salesCloseAt: '2026-11-02T17:10:00Z', salesOpen: true, layoutId: 5,
  };
  const ESKI: YonetimSeansOzeti = {
    ...SEANS, id: 101, movieTitle: 'Eski Film', movieVisible: false, startsAt: '2026-11-02T20:00:00Z',
    salesCloseOffsetMinutes: -15, salesCloseAt: '2026-11-02T19:45:00Z', salesOpen: false, layoutId: 4,
  };
  let fixture: ComponentFixture<SeansYonetimiSayfasiComponent>;
  let seansService: jasmine.SpyObj<SeansYonetimiService>;
  let bildirim: jasmine.SpyObj<BildirimService>;
  let parametreler: BehaviorSubject<ParamMap>;
  let yonlendir: jasmine.Spy;

  beforeAll(() => registerLocaleData(localeTr));

  beforeEach(() => {
    parametreler = new BehaviorSubject(convertToParamMap({ salon: '11', gun: '2026-11-02' }));
    const subeService = jasmine.createSpyObj<SubeYonetimiService>('SubeYonetimiService', ['listele']);
    subeService.listele.and.returnValue(of(SUBELER));
    seansService = jasmine.createSpyObj<SeansYonetimiService>('SeansYonetimiService', ['salonunSeanslari', 'guncelle']);
    seansService.salonunSeanslari.and.returnValue(of([SEANS, ESKI]));
    bildirim = jasmine.createSpyObj<BildirimService>('BildirimService', ['basari']);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [SeansYonetimiSayfasiComponent, SeansDuzenlemeFormuComponent],
      providers: [
        { provide: SubeYonetimiService, useValue: subeService },
        { provide: SeansYonetimiService, useValue: seansService },
        { provide: BildirimService, useValue: bildirim },
        { provide: ActivatedRoute, useValue: { queryParamMap: parametreler } },
        { provide: LOCALE_ID, useValue: 'tr' },
      ],
      // Seans ekleme bileşeni kendi testinde
      schemas: [NO_ERRORS_SCHEMA],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(SeansYonetimiSayfasiComponent);
    fixture.detectChanges();
  });

  function kok(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  function satirlar(): HTMLElement[] {
    return Array.from(kok().querySelectorAll('[data-test="seans-satiri"]'));
  }

  function sonParametreler(): unknown {
    return (yonlendir.calls.mostRecent().args[1] as { queryParams: unknown }).queryParams;
  }

  it('should list day schedule in Istanbul time with hidden film, offset and old layout badges', () => {
    expect(seansService.salonunSeanslari).toHaveBeenCalledWith(11, '2026-11-02');
    expect(satirlar()[0].textContent).toContain('20:00–22:19');
    expect(satirlar()[0].textContent).toContain('satış kapanışı 20:10 (+10 dk)');
    expect(satirlar()[0].querySelector('.bg-success')?.textContent?.trim()).toBe('Satışta');
    expect(satirlar()[0].textContent).not.toContain('Eski düzen');
    expect(satirlar()[1].textContent).toContain('Gizli film');
    expect(satirlar()[1].textContent).toContain('(-15 dk)');
    expect(satirlar()[1].textContent).toContain('Satış kapandı');
    expect(satirlar()[1].textContent).toContain('Eski düzen');
    expect(kok().querySelector('button.btn-primary')?.textContent?.trim()).toBe('Seans ekle');
  });

  it('should fall back to first hall and today, and write selection to address', () => {
    parametreler.next(convertToParamMap({ salon: 'yok', gun: 'dun' }));
    fixture.detectChanges();
    expect(seansService.salonunSeanslari).toHaveBeenCalledWith(11, istanbulGunu(new Date()));

    fixture.componentInstance.subeSecildi(SUBELER, '2');
    expect(sonParametreler()).toEqual({ salon: 21 });
    fixture.componentInstance.gunKaydir('2026-11-30', 1);
    expect(sonParametreler()).toEqual({ gun: '2026-12-01' });
  });

  it('should warn and hide add button for hall without published layout', () => {
    parametreler.next(convertToParamMap({ salon: '12', gun: '2026-11-02' }));
    fixture.detectChanges();

    expect(kok().querySelector('.alert-warning a')?.getAttribute('href')).toBe('/admin/subeler/salonlar/12/duzen');
    expect(Array.from(kok().querySelectorAll('button')).some((b) => b.textContent?.trim() === 'Seans ekle')).toBeFalse();
  });

  it('should edit price and offset, then refresh schedule silently', () => {
    seansService.guncelle.and.returnValue(of({}));
    (satirlar()[0].querySelector('button[aria-label$="seansını düzenle"]') as HTMLButtonElement).click();
    fixture.detectChanges();
    const ofset = kok().querySelector('#seans-duzenle-ofset') as HTMLInputElement;
    expect(ofset.value).toBe('10');

    ofset.value = '-5';
    ofset.dispatchEvent(new Event('input'));
    kok().querySelector('app-seans-duzenleme-formu form')?.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(seansService.guncelle).toHaveBeenCalledWith(100, { ticketPrice: '160.00', salesCloseOffsetMinutes: -5 });
    expect(bildirim.basari).toHaveBeenCalledWith('Son Tren seansı güncellendi.');
    expect(seansService.salonunSeanslari).toHaveBeenCalledTimes(2);
    expect(kok().querySelector('app-seans-duzenleme-formu')).toBeNull();
    expect(kok().querySelector('app-yukleniyor')).toBeNull();
  });

  it('should show schedule load error with retry', () => {
    seansService.salonunSeanslari.and.returnValue(throwError(() => new UygulamaHatasi(503, 'CATALOG_UNAVAILABLE', 'Katalog kullanılamıyor.')));
    parametreler.next(convertToParamMap({ salon: '21', gun: '2026-11-02' }));
    fixture.detectChanges();

    expect(kok().querySelector('app-hata-mesaji')?.textContent).toContain('Katalog kullanılamıyor.');
  });
});
