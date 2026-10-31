import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { LOCALE_ID } from '@angular/core';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SESSIZ_HATA } from '../../core/hata/hata.interceptor';
import { UygulamaHatasi } from '../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../shared/shared.module';
import { BiletDogrulamaSayfasiComponent } from './bilet-dogrulama-sayfasi.component';
import { BiletDogrulamaService, BiletDogrulamasi } from './bilet-dogrulama.service';

describe('BiletDogrulamaSayfasiComponent', () => {
  const BILET: BiletDogrulamasi = {
    valid: true, movieTitle: 'Yıldız Haritası', cinemaName: 'Kadıköy', hallName: 'Salon 1',
    startsAt: '2026-11-01T17:00:00Z', seats: ['D-3', 'D-4'],
  };
  let servis: jasmine.SpyObj<BiletDogrulamaService>;

  beforeAll(() => registerLocaleData(localeTr));

  function olustur(kod: string): HTMLElement {
    TestBed.configureTestingModule({
      imports: [SharedModule],
      declarations: [BiletDogrulamaSayfasiComponent],
      providers: [
        { provide: BiletDogrulamaService, useValue: servis },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ kod })) } },
        { provide: LOCALE_ID, useValue: 'tr' },
      ],
    });
    const fixture = TestBed.createComponent(BiletDogrulamaSayfasiComponent);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  beforeEach(() => {
    servis = jasmine.createSpyObj<BiletDogrulamaService>('BiletDogrulamaService', ['dogrula']);
  });

  it('should show valid ticket details without personal data', () => {
    servis.dogrula.and.returnValue(of(BILET));
    const kok = olustur(' ABCD1234EFGH5678 ');

    expect(servis.dogrula).toHaveBeenCalledWith('ABCD1234EFGH5678');
    expect(kok.textContent).toContain('Geçerli bilet');
    expect(kok.textContent).toContain('1 Kasım Pazar, 20:00');
    expect(kok.textContent).toContain('D-3, D-4');
  });

  it('should show invalid ticket on 404 without retry', () => {
    servis.dogrula.and.returnValue(throwError(() => new UygulamaHatasi(404, 'TICKET_NOT_FOUND', 'Bilet bulunamadı.')));
    const kok = olustur('YANLIS');

    expect(kok.textContent).toContain('Geçersiz bilet');
    expect(kok.querySelector('app-hata-mesaji')).toBeNull();
  });

  it('should show rate limit message with retry', () => {
    servis.dogrula.and.returnValue(throwError(() =>
      new UygulamaHatasi(429, 'RATE_LIMITED', 'Çok fazla istek. 30 saniye sonra tekrar deneyin.', [], 30)));
    const kok = olustur('ABCD1234EFGH5678');

    expect(kok.textContent).toContain('30 saniye sonra');
    expect(kok.querySelector('app-hata-mesaji button')).not.toBeNull();
  });
});

describe('BiletDogrulamaService', () => {
  it('should call public ticket endpoint silently with encoded code', () => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    const sunucu = TestBed.inject(HttpTestingController);

    TestBed.inject(BiletDogrulamaService).dogrula('AB/CD').subscribe();

    const istek = sunucu.expectOne(`${environment.apiTabanAdresi}/tickets/AB%2FCD`);
    expect(istek.request.context.get(SESSIZ_HATA)).toBeTrue();
    sunucu.verify();
  });
});
