import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Route, Router, RouterStateSnapshot, UrlSegment, UrlTree } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { Kullanici, Rol } from './oturum.models';
import { GirisGerekliGuard, MisafirGuard, RolGuard } from './oturum.guards';
import { OturumService } from './oturum.service';

describe('Oturum guards', () => {
  let oturum: { kullanici: Kullanici | null };
  let router: Router;
  const durum = { url: '/rezervasyonlarim' } as RouterStateSnapshot;

  function kullanici(role: Rol): Kullanici {
    return { id: 1, email: 'a@b.com', role, firstName: null, lastName: null, phone: null };
  }

  function rota(rol?: Rol): ActivatedRouteSnapshot {
    return { data: rol ? { rol } : {} } as unknown as ActivatedRouteSnapshot;
  }

  function adres(sonuc: boolean | UrlTree): string {
    return sonuc instanceof UrlTree ? router.serializeUrl(sonuc) : String(sonuc);
  }

  beforeEach(() => {
    oturum = { kullanici: null };
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      providers: [{ provide: OturumService, useValue: oturum }],
    });
    router = TestBed.inject(Router);
  });

  it('should send guest to login with return address', () => {
    expect(adres(TestBed.inject(GirisGerekliGuard).canActivate(rota(), durum))).toBe('/giris?donus=%2Frezervasyonlarim');

    oturum.kullanici = kullanici('USER');
    expect(TestBed.inject(GirisGerekliGuard).canActivate(rota(), durum)).toBeTrue();
  });

  it('should allow only the required role', () => {
    const guard = TestBed.inject(RolGuard);
    expect(adres(guard.canActivate(rota('ADMIN'), durum))).toContain('/giris');

    oturum.kullanici = kullanici('USER');
    expect(adres(guard.canActivate(rota('ADMIN'), durum))).toBe('/');

    oturum.kullanici = kullanici('ADMIN');
    expect(guard.canActivate(rota('ADMIN'), durum)).toBeTrue();
  });

  it('should not load role protected module for guest or wrong role', () => {
    const guard = TestBed.inject(RolGuard);
    const modul: Route = { path: 'admin', data: { rol: 'ADMIN' } };
    const parcalar = [new UrlSegment('admin', {}), new UrlSegment('yoneticiler', {})];

    expect(adres(guard.canLoad(modul, parcalar))).toBe('/giris?donus=%2Fadmin%2Fyoneticiler');

    oturum.kullanici = kullanici('USER');
    expect(adres(guard.canLoad(modul, parcalar))).toBe('/');

    oturum.kullanici = kullanici('ADMIN');
    expect(guard.canLoad(modul, parcalar)).toBeTrue();
  });

  it('should keep logged in user away from login and register pages', () => {
    const guard = TestBed.inject(MisafirGuard);
    expect(guard.canActivate()).toBeTrue();

    oturum.kullanici = kullanici('USER');
    expect(adres(guard.canActivate())).toBe('/');
  });
});
