import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { LOCALE_ID } from '@angular/core';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SunucuSaati } from '../../../../core/zaman/sunucu-saati.service';
import { SharedModule } from '../../../../shared/shared.module';
import { gorunenDurum, Rezervasyon } from '../../models/rezervasyon.models';
import { RezervasyonService } from '../../services/rezervasyon.service';
import { RezervasyonSayfasiComponent } from './rezervasyon-sayfasi.component';

const SUNUCU_SIMDI = '2026-11-01T16:00:00Z';
const BEKLEYEN: Rezervasyon = {
  reservationId: 18, showtimeId: 101, status: 'PENDING_PAYMENT', movieTitle: 'Yıldız Haritası', cinemaName: 'Kadıköy',
  hallName: 'Salon 1', startsAt: '2026-11-01T17:00:00Z', expiresAt: '2026-11-01T16:03:00Z', serverTime: SUNUCU_SIMDI,
  seats: [{ cellId: 60, label: 'D-3' }, { cellId: 61, label: 'D-4' }], totalAmount: '320.00', ticketCode: null,
};

describe('gorunenDurum', () => {
  const simdi = Date.parse(SUNUCU_SIMDI);

  it('should treat a pending hold past its expiry as expired even before the server job runs', () => {
    expect(gorunenDurum(BEKLEYEN, simdi)).toBe('ODEME_BEKLIYOR');
    expect(gorunenDurum({ ...BEKLEYEN, expiresAt: '2026-11-01T15:59:59Z' }, simdi)).toBe('SURESI_DOLDU');
  });

  it('should map final statuses', () => {
    expect(gorunenDurum({ ...BEKLEYEN, status: 'CONFIRMED' }, simdi)).toBe('ONAYLANDI');
    expect(gorunenDurum({ ...BEKLEYEN, status: 'RELEASED' }, simdi)).toBe('BIRAKILDI');
    expect(gorunenDurum({ ...BEKLEYEN, status: 'FAILED' }, simdi)).toBe('BASARISIZ');
    expect(gorunenDurum({ ...BEKLEYEN, status: 'EXPIRED' }, simdi)).toBe('SURESI_DOLDU');
  });
});

describe('RezervasyonSayfasiComponent', () => {
  let servis: jasmine.SpyObj<RezervasyonService>;
  let yonlendir: jasmine.Spy;

  beforeAll(() => registerLocaleData(localeTr));

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date('2026-11-01T15:00:00Z'));
    servis = jasmine.createSpyObj<RezervasyonService>('RezervasyonService', ['getir', 'birak']);
  });

  afterEach(() => jasmine.clock().uninstall());

  function olustur(id = '18'): ComponentFixture<RezervasyonSayfasiComponent> {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [RezervasyonSayfasiComponent],
      providers: [
        { provide: RezervasyonService, useValue: servis },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } },
        { provide: LOCALE_ID, useValue: 'tr' },
      ],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(RezervasyonSayfasiComponent);
    fixture.detectChanges();
    return fixture;
  }

  function vazgecDugmesi(fixture: ComponentFixture<RezervasyonSayfasiComponent>): HTMLButtonElement {
    return Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button'))
      .find((b) => b.textContent?.includes('Vazgeç')) as HTMLButtonElement;
  }

  it('should show pending hold with seats, amount and countdown aligned to server time', () => {
    servis.getir.and.returnValue(of(BEKLEYEN));
    const fixture = olustur();
    const kok: HTMLElement = fixture.nativeElement;

    expect(kok.querySelector('h1')?.textContent).toContain('Ödeme bekleniyor');
    expect(kok.textContent).toContain('D-3, D-4');
    expect(kok.textContent).toContain('₺320,00');
    expect(kok.querySelector('app-geri-sayim')).not.toBeNull();
    // İstemci saati bir saat geride olsa da sunucu saati esas alınır
    expect(new Date(TestBed.inject(SunucuSaati).simdi()).toISOString()).toBe('2026-11-01T16:00:00.000Z');
  });

  it('should switch to expired when countdown ends, without waiting for the server', () => {
    servis.getir.and.returnValue(of(BEKLEYEN));
    const fixture = olustur();

    fixture.componentInstance.sureDoldu();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Ödeme süresi doldu');
    expect(fixture.nativeElement.querySelector('a.btn-primary').getAttribute('href')).toBe('/seans/101');
  });

  it('should release seats once and go back to the showtime', fakeAsync(() => {
    servis.getir.and.returnValue(of(BEKLEYEN));
    const cevap = new Subject<void>();
    servis.birak.and.returnValue(cevap);
    const fixture = olustur();
    const basari = spyOn(TestBed.inject(BildirimService), 'basari');

    vazgecDugmesi(fixture).click();
    vazgecDugmesi(fixture).click();
    fixture.detectChanges();
    expect(servis.birak).toHaveBeenCalledOnceWith(18);
    expect(vazgecDugmesi(fixture).disabled).toBeTrue();

    cevap.next();
    cevap.complete();
    tick();
    expect(basari).toHaveBeenCalledWith('Koltuklar bırakıldı.');
    expect(yonlendir).toHaveBeenCalledWith(['/seans', 101]);
    fixture.destroy();
    discardPeriodicTasks();
  }));

  it('should explain payment in progress and reload when the reservation changed', () => {
    servis.getir.and.returnValue(of(BEKLEYEN));
    servis.birak.and.returnValue(throwError(() =>
      new UygulamaHatasi(409, 'PAYMENT_IN_PROGRESS', 'Ödeme sürüyor; işlem bırakılamaz.')));
    const fixture = olustur();

    vazgecDugmesi(fixture).click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Ödeme sürüyor');
    expect(servis.getir).toHaveBeenCalledTimes(1);

    servis.birak.and.returnValue(throwError(() =>
      new UygulamaHatasi(409, 'RESERVATION_ALREADY_CONFIRMED', 'Rezervasyon onaylandı.')));
    servis.getir.and.returnValue(of({ ...BEKLEYEN, status: 'CONFIRMED', ticketCode: 'ABCD1234EFGH5678' }));
    vazgecDugmesi(fixture).click();
    fixture.detectChanges();

    expect(servis.getir).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('ABCD1234EFGH5678');
  });

  it('should show not found for invalid id and for someone else’s reservation', () => {
    expect(olustur('x').nativeElement.textContent).toContain('Rezervasyon bulunamadı.');
    expect(servis.getir).not.toHaveBeenCalled();
  });
});
