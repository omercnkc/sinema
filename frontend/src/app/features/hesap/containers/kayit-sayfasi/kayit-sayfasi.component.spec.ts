import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { SharedModule } from '../../../../shared/shared.module';
import { KayitService } from '../../services/kayit.service';
import { KayitSayfasiComponent } from './kayit-sayfasi.component';

describe('KayitSayfasiComponent', () => {
  const GECERLI = { firstName: ' Ayşe ', lastName: 'Yılmaz', email: 'ayse@ornek.com', phone: '0532 123 45 67', password: 'Parola123' };
  let fixture: ComponentFixture<KayitSayfasiComponent>;
  let kayit: jasmine.SpyObj<KayitService>;
  let router: Router;

  beforeEach(() => {
    kayit = jasmine.createSpyObj<KayitService>('KayitService', ['kayitOl']);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [KayitSayfasiComponent],
      providers: [{ provide: KayitService, useValue: kayit }],
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(KayitSayfasiComponent);
    fixture.detectChanges();
  });

  function gonder(degerler: typeof GECERLI): void {
    fixture.componentInstance.form.setValue(degerler);
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  function alanHatasi(alan: string): string {
    return fixture.nativeElement.querySelector(`#kayit-${alan}-hata`).textContent.trim();
  }

  it('should validate fields before calling server', () => {
    gonder({ ...GECERLI, phone: '0212 123 45 67', password: 'kisa' });

    expect(kayit.kayitOl).not.toHaveBeenCalled();
    expect(alanHatasi('telefon')).toContain('Geçerli bir cep telefonu');
    expect(alanHatasi('parola')).toContain('en az 8 karakter');
  });

  it('should register with trimmed values and go to login carrying email in state', () => {
    kayit.kayitOl.and.returnValue(of({ id: 9, email: 'ayse@ornek.com' }));

    gonder(GECERLI);

    expect(kayit.kayitOl).toHaveBeenCalledWith({ ...GECERLI, firstName: 'Ayşe' });
    expect(router.navigate).toHaveBeenCalledWith(['/giris'],
      { queryParamsHandling: 'preserve', state: { eposta: 'ayse@ornek.com' } });
  });

  it('should show already registered email under email field', () => {
    kayit.kayitOl.and.returnValue(throwError(() =>
      new UygulamaHatasi(409, 'EMAIL_ALREADY_REGISTERED', 'Bu e-posta adresi zaten kayıtlı.')));

    gonder(GECERLI);

    expect(alanHatasi('eposta')).toBe('Bu e-posta adresi zaten kayıtlı.');
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeNull();
  });

  it('should show server validation errors under fields', () => {
    kayit.kayitOl.and.returnValue(throwError(() => new UygulamaHatasi(400, 'VALIDATION_FAILED', 'Formu düzeltin.', [
      { field: 'lastName', message: 'Soyad yalnızca harf, boşluk, kesme ve tire içerebilir.' },
    ])));

    gonder(GECERLI);

    expect(alanHatasi('soyad')).toBe('Soyad yalnızca harf, boşluk, kesme ve tire içerebilir.');
  });
});
