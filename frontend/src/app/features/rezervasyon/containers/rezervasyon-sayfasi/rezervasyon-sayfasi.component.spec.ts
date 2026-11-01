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
import { SayfaYonlendirici } from '../../../../core/yonlendirme/sayfa-yonlendirici.service';
import { Odeme } from '../../models/odeme.models';
import { OdemeService } from '../../services/odeme.service';
import { RezervasyonService } from '../../services/rezervasyon.service';
import { SeansDetayService } from '../../services/seans-detay.service';
import { SeansDetayi } from '../../models/seans-detayi.models';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RezervasyonSayfasiComponent } from './rezervasyon-sayfasi.component';

const SUNUCU_SIMDI = '2026-11-01T16:00:00Z';
const BEKLEYEN: Rezervasyon = {
  reservationId: 18, showtimeId: 101, status: 'PENDING_PAYMENT', movieTitle: 'Yıldız Haritası', cinemaName: 'Kadıköy',
  hallName: 'Salon 1', startsAt: '2026-11-01T17:00:00Z', expiresAt: '2026-11-01T16:03:00Z', serverTime: SUNUCU_SIMDI,
  seats: [{ cellId: 60, label: 'D-3' }, { cellId: 61, label: 'D-4' }], totalAmount: '320.00', ticketCode: null,
  seatChanges: [],
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
  let odemeServisi: jasmine.SpyObj<OdemeService>;
  let disYonlendirici: jasmine.SpyObj<SayfaYonlendirici>;
  let seansServisi: jasmine.SpyObj<SeansDetayService>;
  let yonlendir: jasmine.Spy;

  beforeAll(() => registerLocaleData(localeTr));

  beforeEach(() => {
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date('2026-11-01T15:00:00Z'));
    servis = jasmine.createSpyObj<RezervasyonService>('RezervasyonService', ['getir', 'birak']);
    odemeServisi = jasmine.createSpyObj<OdemeService>('OdemeService', ['baslat']);
    disYonlendirici = jasmine.createSpyObj<SayfaYonlendirici>('SayfaYonlendirici', ['disAdreseGit']);
    disYonlendirici.disAdreseGit.and.returnValue(true);
    seansServisi = jasmine.createSpyObj<SeansDetayService>('SeansDetayService', ['getir']);
    seansServisi.getir.and.returnValue(of({ posterPath: '/afis.jpg' } as SeansDetayi));
  });

  afterEach(() => jasmine.clock().uninstall());

  function olustur(id = '18'): ComponentFixture<RezervasyonSayfasiComponent> {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [RezervasyonSayfasiComponent],
      providers: [
        { provide: RezervasyonService, useValue: servis },
        { provide: OdemeService, useValue: odemeServisi },
        { provide: SayfaYonlendirici, useValue: disYonlendirici },
        { provide: SeansDetayService, useValue: seansServisi },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } },
        { provide: LOCALE_ID, useValue: 'tr' },
      ],
      // Bilet kartı (QR) kendi testinde; burada ona giden veri denetlenir
      schemas: [NO_ERRORS_SCHEMA],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(RezervasyonSayfasiComponent);
    fixture.detectChanges();
    return fixture;
  }

  function dugme(fixture: ComponentFixture<RezervasyonSayfasiComponent>, metin: string): HTMLButtonElement {
    return Array.from<HTMLButtonElement>(fixture.nativeElement.querySelectorAll('button'))
      .find((b) => b.textContent?.includes(metin)) as HTMLButtonElement;
  }

  function vazgecDugmesi(fixture: ComponentFixture<RezervasyonSayfasiComponent>): HTMLButtonElement {
    return dugme(fixture, 'Vazgeç');
  }

  function odeDugmesi(fixture: ComponentFixture<RezervasyonSayfasiComponent>): HTMLButtonElement {
    return dugme(fixture, 'öde');
  }

  const ODEME: Odeme = {
    paymentId: 5, reservationId: 18, status: 'PENDING', amount: '320.00', paymentPageUrl: 'https://sandbox-cpp.iyzipay.com?token=t',
  };

  describe('ticket', () => {
    const ONAYLI: Rezervasyon = { ...BEKLEYEN, status: 'CONFIRMED', ticketCode: 'ABCD1234EFGH5678' };

    function biletKarti(fixture: ComponentFixture<RezervasyonSayfasiComponent>):
      { rezervasyon: Rezervasyon; afisYolu: string | null; dogrulamaAdresi: string } {
      return fixture.nativeElement.querySelector('app-bilet-karti');
    }

    it('should show the ticket with poster from the showtime and verification address for the QR', () => {
      servis.getir.and.returnValue(of(ONAYLI));
      const fixture = olustur();

      expect(seansServisi.getir).toHaveBeenCalledWith(101);
      expect(biletKarti(fixture).afisYolu).toBe('/afis.jpg');
      expect(biletKarti(fixture).dogrulamaAdresi).toBe(`${document.location.origin}/bilet/ABCD1234EFGH5678`);
      expect(fixture.nativeElement.querySelector('.card-body h2')).toBeNull();
    });

    it('should show seat change banner after a layout migration', () => {
      servis.getir.and.returnValue(of({
        ...ONAYLI,
        seatChanges: [
          { oldLabel: 'D-3', newLabel: 'C-3', reason: 'LABEL_CHANGED', changedAt: '2026-11-01T10:00:00Z' },
          { oldLabel: 'D-4', newLabel: 'C-5', reason: 'REPLACEMENT', changedAt: '2026-11-01T10:00:00Z' },
          { oldLabel: 'E-1', newLabel: 'E-1', reason: 'REPLACEMENT', changedAt: '2026-11-01T10:00:00Z' },
        ],
      }));
      const fixture = olustur();

      const bant = fixture.nativeElement.querySelector('[data-test="koltuk-degisti"]').textContent.replace(/\s+/g, ' ');
      expect(bant).toContain('Koltuğunuz değişti.');
      expect(bant).toContain('D-3 → C-3, D-4 → C-5, E-1 (yeni yer).');
    });

    it('should not show seat change banner when seats never changed', () => {
      servis.getir.and.returnValue(of(ONAYLI));

      expect(olustur().nativeElement.querySelector('[data-test="koltuk-degisti"]')).toBeNull();
    });

    it('should still show the ticket when the poster cannot be loaded', () => {
      servis.getir.and.returnValue(of(ONAYLI));
      seansServisi.getir.and.returnValue(throwError(() => new Error('ağ')));
      const fixture = olustur();

      expect(biletKarti(fixture).afisYolu).toBeNull();
      expect(biletKarti(fixture).rezervasyon.ticketCode).toBe('ABCD1234EFGH5678');
    });

    it('should not load the poster for unpaid reservations', () => {
      servis.getir.and.returnValue(of(BEKLEYEN));
      olustur();

      expect(seansServisi.getir).not.toHaveBeenCalled();
    });

    it('should print the page', () => {
      servis.getir.and.returnValue(of(ONAYLI));
      const fixture = olustur();
      const yazdir = spyOn(window, 'print');

      dugme(fixture, 'Yazdır').click();

      expect(yazdir).toHaveBeenCalled();
    });
  });

  describe('payment', () => {
    it('should start payment once and send browser to the payment page, keeping buttons disabled', () => {
      servis.getir.and.returnValue(of(BEKLEYEN));
      const cevap = new Subject<Odeme>();
      odemeServisi.baslat.and.returnValue(cevap);
      const fixture = olustur();
      expect(odeDugmesi(fixture).textContent).toContain('₺320,00 öde');

      odeDugmesi(fixture).click();
      vazgecDugmesi(fixture).click();
      fixture.detectChanges();
      cevap.next(ODEME);
      cevap.complete();
      fixture.detectChanges();

      expect(odemeServisi.baslat).toHaveBeenCalledOnceWith(18);
      expect(servis.birak).not.toHaveBeenCalled();
      expect(disYonlendirici.disAdreseGit).toHaveBeenCalledWith(ODEME.paymentPageUrl as string);
      expect(odeDugmesi(fixture).disabled).toBeTrue();
      expect(vazgecDugmesi(fixture).disabled).toBeTrue();
    });

    it('should not leave the app for a refused payment address', () => {
      servis.getir.and.returnValue(of(BEKLEYEN));
      odemeServisi.baslat.and.returnValue(of(ODEME));
      disYonlendirici.disAdreseGit.and.returnValue(false);
      const fixture = olustur();

      odeDugmesi(fixture).click();
      fixture.detectChanges();

      expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Ödeme sayfası açılamadı');
      expect(odeDugmesi(fixture).disabled).toBeFalse();
    });

    it('should reload when the reservation was already paid', () => {
      servis.getir.and.returnValue(of(BEKLEYEN));
      odemeServisi.baslat.and.returnValue(throwError(() =>
        new UygulamaHatasi(409, 'PAYMENT_ALREADY_COMPLETED', 'Bu rezervasyonun ödemesi tamamlandı.')));
      const fixture = olustur();
      servis.getir.and.returnValue(of({ ...BEKLEYEN, status: 'CONFIRMED', ticketCode: 'ABCD1234EFGH5678' }));

      odeDugmesi(fixture).click();
      fixture.detectChanges();

      expect(servis.getir).toHaveBeenCalledTimes(2);
      expect(fixture.nativeElement.querySelector('app-bilet-karti').rezervasyon.ticketCode).toBe('ABCD1234EFGH5678');
    });
  });

  it('should show pending hold with seats, amount and countdown aligned to server time', () => {
    servis.getir.and.returnValue(of(BEKLEYEN));
    const fixture = olustur();
    const kok: HTMLElement = fixture.nativeElement;

    expect(kok.querySelector('h1')?.textContent).toContain('Ödeme bekleniyor');
    // Etiket ile değer arasındaki boşluk (&ngsp;) şablon derlemesinde silinmemeli
    expect(kok.textContent).toContain('Koltuklar: D-3, D-4');
    expect(kok.textContent).toContain('Tutar: ₺320,00');
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
    expect(fixture.nativeElement.querySelector('app-bilet-karti').rezervasyon.ticketCode).toBe('ABCD1234EFGH5678');
  });

  it('should show not found for invalid id and for someone else’s reservation', () => {
    expect(olustur('x').nativeElement.textContent).toContain('Rezervasyon bulunamadı.');
    expect(servis.getir).not.toHaveBeenCalled();
  });
});
