import { ComponentFixture, TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeTr from '@angular/common/locales/tr';
import { LOCALE_ID, NO_ERRORS_SCHEMA } from '@angular/core';
import { ActivatedRoute, convertToParamMap, ParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { Sayfa } from '../../../../shared/models/sayfa.models';
import { SharedModule } from '../../../../shared/shared.module';
import { Rezervasyon } from '../../models/rezervasyon.models';
import { RezervasyonService } from '../../services/rezervasyon.service';
import { RezervasyonlarimSayfasiComponent } from './rezervasyonlarim-sayfasi.component';

describe('RezervasyonlarimSayfasiComponent', () => {
  const SUNUCU = '2026-11-01T16:00:00Z';
  function rezervasyon(id: number, degisiklik: Partial<Rezervasyon>): Rezervasyon {
    return {
      reservationId: id, showtimeId: 101, status: 'CONFIRMED', movieTitle: 'Yıldız Haritası', cinemaName: 'Kadıköy',
      hallName: 'Salon 1', startsAt: '2026-11-01T17:00:00Z', expiresAt: '2026-11-01T16:03:00Z', serverTime: SUNUCU,
      seats: [{ cellId: 1, label: 'D-3' }, { cellId: 2, label: 'D-4' }], totalAmount: '320.00', ticketCode: 'K', ...degisiklik,
    };
  }
  let fixture: ComponentFixture<RezervasyonlarimSayfasiComponent>;
  let servis: jasmine.SpyObj<RezervasyonService>;
  let parametreler: BehaviorSubject<ParamMap>;

  beforeAll(() => registerLocaleData(localeTr));

  beforeEach(() => {
    servis = jasmine.createSpyObj<RezervasyonService>('RezervasyonService', ['listele']);
    parametreler = new BehaviorSubject(convertToParamMap({}));
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [RezervasyonlarimSayfasiComponent],
      providers: [
        { provide: RezervasyonService, useValue: servis },
        { provide: ActivatedRoute, useValue: { queryParamMap: parametreler } },
        { provide: LOCALE_ID, useValue: 'tr' },
      ],
      // ngb-pagination bu testin konusu değil
      schemas: [NO_ERRORS_SCHEMA],
    });
    fixture = TestBed.createComponent(RezervasyonlarimSayfasiComponent);
  });

  function sayfa(icerik: Rezervasyon[], toplam = icerik.length): Sayfa<Rezervasyon> {
    return { content: icerik, page: 0, size: 10, totalElements: toplam, totalPages: Math.ceil(toplam / 10) };
  }

  it('should list reservations with status badge, seats and link to each', () => {
    servis.listele.and.returnValue(of(sayfa([
      rezervasyon(2, {}),
      // Süresi geçmiş ama sunucu job'ı henüz işlememiş: "süresi doldu" görünür
      rezervasyon(1, { status: 'PENDING_PAYMENT', expiresAt: '2026-11-01T15:59:00Z', ticketCode: null }),
    ])));
    fixture.detectChanges();
    const satirlar = Array.from<HTMLAnchorElement>(fixture.nativeElement.querySelectorAll('a.list-group-item'));

    expect(servis.listele).toHaveBeenCalledWith(0);
    expect(satirlar.map((a) => a.getAttribute('href'))).toEqual(['/rezervasyonlarim/2', '/rezervasyonlarim/1']);
    expect(satirlar[0].querySelector('.badge')?.textContent).toBe('Onaylandı');
    expect(satirlar[1].querySelector('.badge')?.textContent).toBe('Süresi doldu');
    expect(satirlar[0].textContent).toContain('D-3, D-4');
    expect(satirlar[0].textContent).toContain('₺320,00');
  });

  it('should read page from address and write page changes back', () => {
    servis.listele.and.returnValue(of(sayfa([], 25)));
    const yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    parametreler.next(convertToParamMap({ sayfa: '3' }));
    fixture.detectChanges();

    expect(servis.listele).toHaveBeenCalledWith(2);
    fixture.componentInstance.sayfaDegisti(1);
    expect(yonlendir.calls.mostRecent().args[1]).toEqual(jasmine.objectContaining({ queryParams: { sayfa: null } }));
  });

  it('should invite to browse films when there are no reservations', () => {
    servis.listele.and.returnValue(of(sayfa([])));
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Henüz rezervasyonunuz yok.');
  });

  it('should offer retry on error', () => {
    servis.listele.and.returnValue(throwError(() => new UygulamaHatasi(503, 'HTTP_503', 'Hizmet şu an kullanılamıyor.')));
    fixture.detectChanges();
    servis.listele.and.returnValue(of(sayfa([rezervasyon(2, {})])));

    fixture.nativeElement.querySelector('app-hata-mesaji button').click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('a.list-group-item').length).toBe(1);
  });
});
