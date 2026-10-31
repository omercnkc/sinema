import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { LOCALE_ID, NO_ERRORS_SCHEMA } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of, Subject, throwError } from 'rxjs';
import { environment } from '../../../../../environments/environment';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { Kullanici } from '../../../../core/oturum/oturum.models';
import { OturumService } from '../../../../core/oturum/oturum.service';
import { SekmeGorunurlugu } from '../../../../core/zaman/sekme-gorunurlugu';
import { SharedModule } from '../../../../shared/shared.module';
import { DolulukCevabi, HaritaHucresi, Hucre, KoltukHaritasiGorunumu, SeansDuzeni } from '../../models/koltuk-haritasi.models';
import { ornekDuzen } from '../../models/koltuk-haritasi.models.spec';
import { Rezervasyon } from '../../models/rezervasyon.models';
import { SeansDetayi } from '../../models/seans-detayi.models';
import { KoltukHaritasiService } from '../../services/koltuk-haritasi.service';
import { RezervasyonService } from '../../services/rezervasyon.service';
import { KoltukSecimiComponent } from './koltuk-secimi.component';

describe('KoltukSecimiComponent', () => {
  const ARALIK = environment.dolulukYenilemeMs;
  const KULLANICI: Kullanici = { id: 1, email: 'a@b.com', role: 'USER', firstName: null, lastName: null, phone: null };
  const SEANS = { id: 101, ticketPrice: '160.50' } as SeansDetayi;
  let fixture: ComponentFixture<KoltukSecimiComponent>;
  let haritaServisi: jasmine.SpyObj<KoltukHaritasiService>;
  let rezervasyonServisi: jasmine.SpyObj<RezervasyonService>;
  let gorunur: BehaviorSubject<boolean>;
  let kullanici: BehaviorSubject<Kullanici | null>;
  let yonlendir: jasmine.Spy;

  function doluluk(...tutulular: number[]): DolulukCevabi {
    return { serverTime: '', occupied: tutulular.map((cellId) => ({ cellId, status: 'HELD' as const })) };
  }

  /** Örnek düzene 4 koltuk daha eklenir (5 koltuk sınırı için). */
  function genisDuzen(): SeansDuzeni {
    const ek: Hucre[] = [40, 41, 42, 43].map((cellId, sira) => ({
      cellId, row: 2, col: sira + 5, type: 'SEAT', seatRowLabel: 'B', seatNumber: sira + 2, seatType: 'STANDARD',
    }));
    const duzen = ornekDuzen();
    return { ...duzen, gridCols: 9, cells: [...duzen.cells, ...ek] };
  }

  beforeAll(() => registerLocaleData(localeTr));

  beforeEach(() => {
    haritaServisi = jasmine.createSpyObj<KoltukHaritasiService>('KoltukHaritasiService', ['duzen', 'doluluk']);
    haritaServisi.duzen.and.returnValue(of(genisDuzen()));
    haritaServisi.doluluk.and.returnValue(of(doluluk()));
    rezervasyonServisi = jasmine.createSpyObj<RezervasyonService>('RezervasyonService', ['tut']);
    gorunur = new BehaviorSubject<boolean>(true);
    kullanici = new BehaviorSubject<Kullanici | null>(KULLANICI);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [KoltukSecimiComponent],
      providers: [
        { provide: KoltukHaritasiService, useValue: haritaServisi },
        { provide: RezervasyonService, useValue: rezervasyonServisi },
        { provide: SekmeGorunurlugu, useValue: { gorunur$: gorunur } },
        { provide: OturumService, useValue: { kullanici$: kullanici } },
        { provide: LOCALE_ID, useValue: 'tr' },
      ],
      // Haritanın çizimi kendi testinde; burada ona giden görünüm ve tıklama davranışı denetlenir
      schemas: [NO_ERRORS_SCHEMA],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(KoltukSecimiComponent);
    fixture.componentInstance.seans = SEANS;
  });

  function baslat(): void {
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  function bitir(): void {
    fixture.destroy();
    discardPeriodicTasks();
  }

  /** Sayfanın haritaya verdiği görünüm (NO_ERRORS_SCHEMA'da bağlama DOM özelliğine yazılır). */
  function haritaGorunumu(): KoltukHaritasiGorunumu {
    return (fixture.nativeElement.querySelector('app-koltuk-haritasi') as { gorunum: KoltukHaritasiGorunumu }).gorunum;
  }

  function hucre(id: number): HaritaHucresi {
    return haritaGorunumu().hucreler.find((h) => h.hucre.cellId === id) as HaritaHucresi;
  }

  function tikla(...idler: number[]): void {
    idler.forEach((id) => fixture.componentInstance.koltukTiklandi(hucre(id), kullanici.value));
    fixture.detectChanges();
  }

  function ozet(): string {
    return fixture.nativeElement.querySelector('.card').textContent;
  }

  function devamDugmesi(): HTMLButtonElement {
    return fixture.nativeElement.querySelector('.card button');
  }

  describe('occupancy', () => {
    it('should poll immediately and then every interval, loading layout once', fakeAsync(() => {
      baslat();
      haritaServisi.doluluk.and.returnValue(of(doluluk(10)));
      tick(ARALIK);
      fixture.detectChanges();

      expect(haritaServisi.doluluk).toHaveBeenCalledTimes(2);
      expect(haritaServisi.duzen).toHaveBeenCalledTimes(1);
      expect(hucre(10).durum).toBe('TUTULU');
      bitir();
    }));

    it('should stop polling while tab is hidden and refresh as soon as it is visible again', fakeAsync(() => {
      baslat();
      gorunur.next(false);
      tick(ARALIK * 3);
      expect(haritaServisi.doluluk).toHaveBeenCalledTimes(1);

      gorunur.next(true);
      tick();
      expect(haritaServisi.doluluk).toHaveBeenCalledTimes(2);
      bitir();
    }));

    it('should keep last occupancy and warn when refresh fails', fakeAsync(() => {
      haritaServisi.doluluk.and.returnValue(of(doluluk(10)));
      baslat();
      haritaServisi.doluluk.and.returnValue(throwError(() => new Error('ağ')));
      tick(ARALIK);
      fixture.detectChanges();

      expect(hucre(10).durum).toBe('TUTULU');
      expect(fixture.nativeElement.textContent).toContain('Koltuk durumu güncellenemedi');
      bitir();
    }));
  });

  describe('selection', () => {
    it('should send guests to login instead of selecting', fakeAsync(() => {
      kullanici.next(null);
      baslat();

      expect(fixture.nativeElement.querySelector('a[href^="/giris"]').getAttribute('href')).toBe('/giris?donus=%2Fseans%2F101');
      tikla(10);

      expect(yonlendir).toHaveBeenCalledWith(['/giris'], { queryParams: { donus: '/seans/101' } });
      expect(fixture.nativeElement.querySelector('.card')).toBeNull();
      bitir();
    }));

    it('should toggle free seats, ignore occupied ones and show labels with total', fakeAsync(() => {
      haritaServisi.doluluk.and.returnValue(of(doluluk(11)));
      baslat();

      tikla(10, 20, 11);
      expect(ozet()).toContain('2 koltuk: A-1, B-1');
      expect(ozet()).toContain('₺321');

      tikla(20);
      expect(hucre(20).durum).toBe('BOS');
      expect(ozet()).toContain('1 koltuk: A-1');
      bitir();
    }));

    it('should allow at most 5 seats', fakeAsync(() => {
      const bilgi = spyOn(TestBed.inject(BildirimService), 'bilgi');
      baslat();

      tikla(10, 11, 20, 40, 41, 42);

      expect(haritaGorunumu().hucreler.filter((h) => h.durum === 'SECILI').length).toBe(5);
      expect(bilgi).toHaveBeenCalledWith('Bir işlemde en çok 5 koltuk seçebilirsiniz.');
      bitir();
    }));

    it('should drop a selected seat when someone else takes it', fakeAsync(() => {
      baslat();
      tikla(10, 11);

      haritaServisi.doluluk.and.returnValue(of(doluluk(11)));
      tick(ARALIK);
      fixture.detectChanges();

      expect(ozet()).toContain('1 koltuk: A-1');
      bitir();
    }));
  });

  describe('hold', () => {
    const REZERVASYON = { reservationId: 77 } as Rezervasyon;

    it('should hold selected seats once and go to the reservation page', fakeAsync(() => {
      const cevap = new Subject<Rezervasyon>();
      rezervasyonServisi.tut.and.returnValue(cevap);
      baslat();
      tikla(10, 20);

      devamDugmesi().click();
      devamDugmesi().click();
      fixture.detectChanges();
      expect(rezervasyonServisi.tut).toHaveBeenCalledOnceWith({ showtimeId: 101, seatCellIds: [10, 20] });
      expect(devamDugmesi().disabled).toBeTrue();

      cevap.next(REZERVASYON);
      cevap.complete();
      expect(yonlendir).toHaveBeenCalledWith(['/rezervasyonlarim', 77]);
      bitir();
    }));

    it('should remove taken seats, refresh map at once and explain on 409 SEATS_UNAVAILABLE', fakeAsync(() => {
      rezervasyonServisi.tut.and.returnValue(throwError(() => new UygulamaHatasi(409, 'SEATS_UNAVAILABLE',
        'Seçtiğiniz koltuklardan bazıları az önce alındı.', [], null, null, { unavailableCellIds: [20] })));
      baslat();
      tikla(10, 20);
      const oncekiYoklama = haritaServisi.doluluk.calls.count();

      devamDugmesi().click();
      fixture.detectChanges();

      expect(haritaServisi.doluluk.calls.count()).toBe(oncekiYoklama + 1);
      expect(hucre(20).durum).toBe('BOS');
      expect(hucre(10).durum).toBe('SECILI');
      expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('az önce alındı');
      expect(devamDugmesi().disabled).toBeFalse();
      bitir();
    }));

    it('should link to the pending reservation on 409 ACTIVE_RESERVATION_EXISTS', fakeAsync(() => {
      rezervasyonServisi.tut.and.returnValue(throwError(() => new UygulamaHatasi(409, 'ACTIVE_RESERVATION_EXISTS',
        'Bu seans için ödemesi bekleyen bir işleminiz var.', [], null, null, { reservationId: 55 })));
      baslat();
      tikla(10);

      devamDugmesi().click();
      fixture.detectChanges();

      const baglanti: HTMLAnchorElement = fixture.nativeElement.querySelector('[role="alert"] a');
      expect(baglanti.getAttribute('href')).toBe('/rezervasyonlarim/55');
      bitir();
    }));

    it('should show cooldown message with remaining time and clear it when selection changes', fakeAsync(() => {
      rezervasyonServisi.tut.and.returnValue(throwError(() => new UygulamaHatasi(429, 'HOLD_COOLDOWN',
        'Çok sayıda işlem yarım bırakıldı. 8 dakika sonra tekrar deneyin.', [], 480)));
      baslat();
      tikla(10);

      devamDugmesi().click();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('8 dakika sonra');

      tikla(20);
      expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
      bitir();
    }));
  });
});
