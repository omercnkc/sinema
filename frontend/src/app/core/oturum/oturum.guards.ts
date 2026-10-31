import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Rol } from './oturum.models';
import { OturumService } from './oturum.service';

/*
 * Rota koruyucuları (class tabanlı, Angular 13; rules §6.1). Oturum APP_INITIALIZER'da kurulduğu için anlık değer
 * yeterli, beklemeye gerek yok. Asıl yetki kontrolü sunucudadır (gateway + servisler); bunlar yalnızca kullanıcıyı
 * doğru sayfaya yönlendirir.
 */

/** Giriş gerektiren sayfalar: misafir, dönüş adresiyle giriş sayfasına gider. */
@Injectable({ providedIn: 'root' })
export class GirisGerekliGuard implements CanActivate {
  constructor(private readonly oturumService: OturumService, private readonly router: Router) {}

  canActivate(_: ActivatedRouteSnapshot, durum: RouterStateSnapshot): boolean | UrlTree {
    return this.oturumService.kullanici !== null || girisSayfasi(this.router, durum.url);
  }
}

/** Rota verisindeki rolü (`data: { rol: 'ADMIN' }`) ister; rolü uymayan giriş yapmış kullanıcı ana sayfaya döner. */
@Injectable({ providedIn: 'root' })
export class RolGuard implements CanActivate {
  constructor(private readonly oturumService: OturumService, private readonly router: Router) {}

  canActivate(rota: ActivatedRouteSnapshot, durum: RouterStateSnapshot): boolean | UrlTree {
    const kullanici = this.oturumService.kullanici;
    if (kullanici === null) {
      return girisSayfasi(this.router, durum.url);
    }
    const istenenRol = rota.data['rol'] as Rol | undefined;
    return istenenRol === undefined || kullanici.role === istenenRol || this.router.parseUrl('/');
  }
}

/** Giriş ve kayıt sayfaları: zaten giriş yapmış kullanıcı ana sayfaya gider. */
@Injectable({ providedIn: 'root' })
export class MisafirGuard implements CanActivate {
  constructor(private readonly oturumService: OturumService, private readonly router: Router) {}

  canActivate(): boolean | UrlTree {
    return this.oturumService.kullanici === null || this.router.parseUrl('/');
  }
}

function girisSayfasi(router: Router, donus: string): UrlTree {
  return router.createUrlTree(['/giris'], { queryParams: { donus } });
}
