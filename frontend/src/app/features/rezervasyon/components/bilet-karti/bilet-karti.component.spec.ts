import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { LOCALE_ID } from '@angular/core';
import { QRCodeModule } from 'angularx-qrcode';
import { SharedModule } from '../../../../shared/shared.module';
import { Rezervasyon } from '../../models/rezervasyon.models';
import { BiletKartiComponent } from './bilet-karti.component';

describe('BiletKartiComponent', () => {
  const REZERVASYON: Rezervasyon = {
    reservationId: 18, showtimeId: 101, status: 'CONFIRMED', movieTitle: 'Yıldız Haritası', cinemaName: 'Kadıköy',
    hallName: 'Salon 1', startsAt: '2026-11-01T17:00:00Z', expiresAt: '2026-11-01T16:03:00Z',
    serverTime: '2026-11-01T16:00:00Z', seats: [{ cellId: 60, label: 'D-3' }, { cellId: 61, label: 'D-4' }],
    totalAmount: '320.00', ticketCode: 'ABCD1234EFGH5678',
  };
  let fixture: ComponentFixture<BiletKartiComponent>;

  beforeAll(() => registerLocaleData(localeTr));

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [SharedModule, QRCodeModule],
      declarations: [BiletKartiComponent],
      providers: [{ provide: LOCALE_ID, useValue: 'tr' }],
    });
    fixture = TestBed.createComponent(BiletKartiComponent);
    fixture.componentInstance.rezervasyon = REZERVASYON;
    fixture.componentInstance.dogrulamaAdresi = 'http://localhost:4200/bilet/ABCD1234EFGH5678';
    fixture.detectChanges();
    fixture.whenStable().then(() => fixture.detectChanges());
  }));

  it('should show ticket details without personal data', () => {
    const metin: string = fixture.nativeElement.textContent;

    expect(metin).toContain('Yıldız Haritası');
    expect(metin).toContain('1 Kasım 2026 Pazar, 20:00');
    expect(metin).toContain('Kadıköy · Salon 1');
    expect(metin).toContain('D-3, D-4');
    expect(metin).toContain('₺320,00');
    expect(metin).toContain('ABCD1234EFGH5678');
  });

  it('should draw a QR code carrying the verification address', () => {
    const qr = fixture.nativeElement.querySelector('[role="img"][aria-label^="Bilet doğrulama QR"]');

    expect(qr.getAttribute('aria-label')).toContain('ABCD1234EFGH5678');
    expect(qr.querySelector('svg')).not.toBeNull();
  });
});
