import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Params, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { SharedModule } from '../../../../shared/shared.module';
import { Odeme } from '../../models/odeme.models';
import { Rezervasyon } from '../../models/rezervasyon.models';
import { OdemeService } from '../../services/odeme.service';
import { RezervasyonService } from '../../services/rezervasyon.service';
import { OdemeSonucuSayfasiComponent } from './odeme-sonucu-sayfasi.component';

describe('OdemeSonucuSayfasiComponent', () => {
  const ARALIK = 2000;
  const BEKLEYEN = { reservationId: 18, showtimeId: 101, status: 'PENDING_PAYMENT' } as Rezervasyon;
  let rezervasyonServisi: jasmine.SpyObj<RezervasyonService>;
  let odemeServisi: jasmine.SpyObj<OdemeService>;
  let yonlendir: jasmine.Spy;
  let fixture: ComponentFixture<OdemeSonucuSayfasiComponent>;

  function olustur(parametreler: Params): void {
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [OdemeSonucuSayfasiComponent],
      providers: [
        { provide: RezervasyonService, useValue: rezervasyonServisi },
        { provide: OdemeService, useValue: odemeServisi },
        { provide: ActivatedRoute, useValue: { queryParamMap: of(convertToParamMap(parametreler)) } },
      ],
    });
    yonlendir = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(OdemeSonucuSayfasiComponent);
    fixture.detectChanges();
  }

  function ilerle(ms: number): void {
    tick(ms);
    fixture.detectChanges();
  }

  function metin(): string {
    return fixture.nativeElement.textContent;
  }

  beforeEach(() => {
    rezervasyonServisi = jasmine.createSpyObj<RezervasyonService>('RezervasyonService', ['getir']);
    odemeServisi = jasmine.createSpyObj<OdemeService>('OdemeService', ['rezervasyonunOdemesi']);
  });

  it('should go to the ticket when reservation is confirmed', fakeAsync(() => {
    rezervasyonServisi.getir.and.returnValues(of(BEKLEYEN), of({ ...BEKLEYEN, status: 'CONFIRMED' }));
    olustur({ reservationId: '18' });
    const basari = spyOn(TestBed.inject(BildirimService), 'basari');

    ilerle(0);
    expect(metin()).toContain('kontrol ediliyor');
    ilerle(ARALIK);

    expect(rezervasyonServisi.getir).toHaveBeenCalledTimes(2);
    expect(basari).toHaveBeenCalledWith('Ödemeniz alındı, biletiniz hazır.');
    expect(yonlendir).toHaveBeenCalledWith(['/rezervasyonlarim', 18], { replaceUrl: true });
    ilerle(ARALIK * 3);
    expect(rezervasyonServisi.getir).toHaveBeenCalledTimes(2);
  }));

  it('should stop after limited attempts and let the user check again', fakeAsync(() => {
    rezervasyonServisi.getir.and.returnValue(of(BEKLEYEN));
    olustur({ reservationId: '18' });

    ilerle(0);
    ilerle(ARALIK * 14);
    expect(rezervasyonServisi.getir).toHaveBeenCalledTimes(15);
    expect(metin()).toContain('hâlâ kontrol ediliyor');
    ilerle(ARALIK * 5);
    expect(rezervasyonServisi.getir).toHaveBeenCalledTimes(15);

    (fixture.nativeElement.querySelector('button') as HTMLButtonElement).click();
    ilerle(0);
    expect(rezervasyonServisi.getir).toHaveBeenCalledTimes(16);
    ilerle(ARALIK * 14);
  }));

  it('should keep polling through temporary errors', fakeAsync(() => {
    rezervasyonServisi.getir.and.returnValues(throwError(() => new Error('ağ')), of({ ...BEKLEYEN, status: 'CONFIRMED' }));
    olustur({ reservationId: '18' });

    ilerle(0);
    ilerle(ARALIK);

    expect(yonlendir).toHaveBeenCalledWith(['/rezervasyonlarim', 18], { replaceUrl: true });
  }));

  it('should tell about refund when payment arrived after the hold expired', fakeAsync(() => {
    rezervasyonServisi.getir.and.returnValue(of({ ...BEKLEYEN, status: 'EXPIRED' }));
    odemeServisi.rezervasyonunOdemesi.and.returnValue(of({ status: 'REFUND_REQUIRED' } as Odeme));
    olustur({ reservationId: '18' });

    ilerle(0);

    expect(metin()).toContain('Ödeme süresi dolduğu için biletiniz oluşturulamadı');
    expect(metin()).toContain('kartınıza iade edilir');
    expect(fixture.nativeElement.querySelector('a.btn-primary').getAttribute('href')).toBe('/seans/101');
  }));

  it('should not claim a refund when the payment simply failed', fakeAsync(() => {
    rezervasyonServisi.getir.and.returnValue(of({ ...BEKLEYEN, status: 'FAILED' }));
    odemeServisi.rezervasyonunOdemesi.and.returnValue(of({ status: 'FAILED' } as Odeme));
    olustur({ reservationId: '18' });

    ilerle(0);

    expect(metin()).toContain('Ödeme tamamlanamadı');
    expect(metin()).not.toContain('iade');
  }));

  it('should show not found for unknown payment token without polling', fakeAsync(() => {
    olustur({ error: 'PAYMENT_NOT_FOUND' });
    ilerle(0);

    expect(metin()).toContain('Ödeme kaydı bulunamadı');
    expect(rezervasyonServisi.getir).not.toHaveBeenCalled();
  }));
});
