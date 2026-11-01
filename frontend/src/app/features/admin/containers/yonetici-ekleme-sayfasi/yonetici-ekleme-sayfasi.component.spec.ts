import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { YoneticiCevabi } from '../../models/yonetici.models';
import { YoneticiService } from '../../services/yonetici.service';
import { YoneticiEklemeSayfasiComponent } from './yonetici-ekleme-sayfasi.component';

describe('YoneticiEklemeSayfasiComponent', () => {
  const GECERLI = { email: 'yeni@sinema.local', password: 'Parola123' };
  let fixture: ComponentFixture<YoneticiEklemeSayfasiComponent>;
  let yonetici: jasmine.SpyObj<YoneticiService>;
  let bildirim: jasmine.SpyObj<BildirimService>;

  beforeEach(() => {
    yonetici = jasmine.createSpyObj<YoneticiService>('YoneticiService', ['ekle']);
    bildirim = jasmine.createSpyObj<BildirimService>('BildirimService', ['basari']);
    TestBed.configureTestingModule({
      imports: [SharedModule],
      declarations: [YoneticiEklemeSayfasiComponent],
      providers: [
        { provide: YoneticiService, useValue: yonetici },
        { provide: BildirimService, useValue: bildirim },
      ],
    });
    fixture = TestBed.createComponent(YoneticiEklemeSayfasiComponent);
    fixture.detectChanges();
  });

  function formuGonder(): void {
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function gonder(degerler: typeof GECERLI): void {
    fixture.componentInstance.form.setValue(degerler);
    formuGonder();
  }

  function alanHatasi(alan: string): string {
    return fixture.nativeElement.querySelector(`#yonetici-${alan}-hata`).textContent.trim();
  }

  it('should validate fields before calling server', () => {
    gonder({ email: 'gecersiz', password: 'kisa' });

    expect(yonetici.ekle).not.toHaveBeenCalled();
    expect(alanHatasi('eposta')).toContain('Geçerli bir e-posta');
    expect(alanHatasi('parola')).toContain('en az 8 karakter');
  });

  it('should add admin, notify and clear form', () => {
    yonetici.ekle.and.returnValue(of({ id: 7, email: 'yeni@sinema.local' }));

    gonder(GECERLI);

    expect(yonetici.ekle).toHaveBeenCalledWith({ email: 'yeni@sinema.local', password: 'Parola123' });
    expect(bildirim.basari).toHaveBeenCalledWith('yeni@sinema.local yönetici olarak eklendi.');
    expect(fixture.componentInstance.form.value).toEqual({ email: '', password: '' });
    expect(alanHatasi('eposta')).toBe('');
  });

  it('should show already registered email under email field', () => {
    yonetici.ekle.and.returnValue(throwError(() =>
      new UygulamaHatasi(409, 'EMAIL_ALREADY_REGISTERED', 'Bu e-posta adresi zaten kayıtlı.')));

    gonder(GECERLI);

    expect(alanHatasi('eposta')).toBe('Bu e-posta adresi zaten kayıtlı.');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('should show other errors above the form', () => {
    yonetici.ekle.and.returnValue(throwError(() => new UygulamaHatasi(403, 'FORBIDDEN', 'Bu işlem için yetkiniz yok.')));

    gonder(GECERLI);

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('Bu işlem için yetkiniz yok.');
  });

  it('should send one request while submitting and disable button', () => {
    yonetici.ekle.and.returnValue(new Subject<YoneticiCevabi>());

    gonder(GECERLI);
    formuGonder();

    expect(yonetici.ekle).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
  });
});
