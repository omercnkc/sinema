import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { LOCALE_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of } from 'rxjs';
import { SharedModule } from '../../../../shared/shared.module';
import { SeansDetayi } from '../../models/seans-detayi.models';
import { SeansDetayService } from '../../services/seans-detay.service';
import { SeansSayfasiComponent } from './seans-sayfasi.component';

describe('SeansSayfasiComponent', () => {
  const SEANS: SeansDetayi = {
    id: 12, movieId: 3, movieTitle: 'Yıldız Haritası', posterPath: null, certification: '13+', cinemaId: 1,
    cinemaName: 'Kadıköy', hallId: 1, hallName: 'Salon 1', layoutId: 1, startsAt: '2026-11-01T17:00:00Z',
    endsAt: '2026-11-01T19:37:00Z', ticketPrice: '160.00', salesCloseOffsetMinutes: 10,
    salesCloseAt: '2026-11-01T17:10:00Z', salesOpen: true, serverTime: '2026-11-01T09:00:00Z',
  };
  let servis: jasmine.SpyObj<SeansDetayService>;

  beforeAll(() => registerLocaleData(localeTr));

  function olustur(id: string): HTMLElement {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [SeansSayfasiComponent],
      providers: [
        { provide: SeansDetayService, useValue: servis },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id })) } },
        { provide: LOCALE_ID, useValue: 'tr' },
      ],
    });
    const fixture = TestBed.createComponent(SeansSayfasiComponent);
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  beforeEach(() => {
    servis = jasmine.createSpyObj<SeansDetayService>('SeansDetayService', ['getir']);
  });

  it('should show showtime header in Istanbul time with price', () => {
    servis.getir.and.returnValue(of(SEANS));
    const kok = olustur('12');

    expect(servis.getir).toHaveBeenCalledWith(12);
    expect(kok.textContent).toContain('1 Kasım Pazar, 20:00');
    expect(kok.textContent).toContain('Kadıköy · Salon 1');
    expect(kok.textContent).toContain('₺160');
    expect(kok.textContent).toContain('Satış kapanışı: 20:10');
  });

  it('should tell when sales are closed', () => {
    servis.getir.and.returnValue(of({ ...SEANS, salesOpen: false }));

    expect(olustur('12').textContent).toContain('Bu seansın satışı kapandı.');
  });

  it('should show not found for invalid id without calling server', () => {
    const kok = olustur('x');

    expect(servis.getir).not.toHaveBeenCalled();
    expect(kok.textContent).toContain('Seans bulunamadı.');
  });
});
