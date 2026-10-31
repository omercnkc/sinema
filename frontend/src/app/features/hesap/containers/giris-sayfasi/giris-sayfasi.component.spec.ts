import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, Subject, throwError } from 'rxjs';
import { BildirimService } from '../../../../core/bildirim/bildirim.service';
import { UygulamaHatasi } from '../../../../core/hata/uygulama-hatasi';
import { Kullanici } from '../../../../core/oturum/oturum.models';
import { OturumService } from '../../../../core/oturum/oturum.service';
import { SharedModule } from '../../../../shared/shared.module';
import { GirisSayfasiComponent } from './giris-sayfasi.component';

describe('GirisSayfasiComponent', () => {
  const KULLANICI: Kullanici = { id: 1, email: 'ayse@ornek.com', role: 'USER', firstName: 'Ayşe', lastName: null, phone: null };
  let fixture: ComponentFixture<GirisSayfasiComponent>;
  let oturum: jasmine.SpyObj<OturumService>;
  let router: Router;
  let donus: string | null;

  beforeEach(() => {
    donus = null;
    oturum = jasmine.createSpyObj<OturumService>('OturumService', ['girisYap']);
    TestBed.configureTestingModule({
      imports: [RouterTestingModule, SharedModule],
      declarations: [GirisSayfasiComponent],
      providers: [
        { provide: OturumService, useValue: oturum },
        { provide: ActivatedRoute, useValue: { snapshot: { get queryParamMap() { return convertToParamMap(donus ? { donus } : {}); } } } },
      ],
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture = TestBed.createComponent(GirisSayfasiComponent);
    fixture.detectChanges();
  });

  function doldurVeGonder(eposta: string, parola: string): void {
    fixture.componentInstance.form.setValue({ email: eposta, password: parola });
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  }

  it('should not call server and show field errors when form is empty', () => {
    doldurVeGonder('', '');

    expect(oturum.girisYap).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('#giris-eposta-hata').textContent).toContain('E-posta zorunludur.');
  });

  it('should log in, greet and go to return address', () => {
    donus = '/rezervasyonlarim';
    oturum.girisYap.and.returnValue(of(KULLANICI));
    const basari = spyOn(TestBed.inject(BildirimService), 'basari');

    doldurVeGonder('ayse@ornek.com', 'Parola123');

    expect(oturum.girisYap).toHaveBeenCalledWith({ email: 'ayse@ornek.com', password: 'Parola123' });
    expect(basari).toHaveBeenCalledWith('Hoş geldiniz, Ayşe.');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/rezervasyonlarim');
  });

  it('should ignore second submit while login is in progress', () => {
    oturum.girisYap.and.returnValue(new Subject<Kullanici>());

    doldurVeGonder('ayse@ornek.com', 'Parola123');
    doldurVeGonder('ayse@ornek.com', 'Parola123');

    expect(oturum.girisYap).toHaveBeenCalledTimes(1);
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeTrue();
  });

  it('should show account lock message with remaining time', () => {
    oturum.girisYap.and.returnValue(throwError(() =>
      new UygulamaHatasi(429, 'ACCOUNT_LOCKED', 'Çok fazla hatalı deneme. 5 dakika sonra tekrar deneyin.', [], 300)));

    doldurVeGonder('ayse@ornek.com', 'yanlis-parola');

    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('5 dakika sonra');
    expect(fixture.nativeElement.querySelector('button[type="submit"]').disabled).toBeFalse();
  });

  it('should only allow in-app return addresses', () => {
    expect(GirisSayfasiComponent.guvenliDonusAdresi('/filmler/3')).toBe('/filmler/3');
    expect(GirisSayfasiComponent.guvenliDonusAdresi('//kotu.example')).toBe('/');
    expect(GirisSayfasiComponent.guvenliDonusAdresi('/\\kotu.example')).toBe('/');
    expect(GirisSayfasiComponent.guvenliDonusAdresi('https://kotu.example')).toBe('/');
    expect(GirisSayfasiComponent.guvenliDonusAdresi(null)).toBe('/');
  });
});
