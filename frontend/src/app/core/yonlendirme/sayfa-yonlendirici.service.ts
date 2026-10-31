import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';

const IZINLI_PROTOKOLLER = new Set(['https:', 'http:']);

/**
 * Uygulama dışına tam sayfa yönlendirme (ödeme sayfası, K41: iframe yok). `window.location` doğrudan
 * kullanılmaz: testte sahtelenir ve yalnızca http(s) adreslere izin verilir (sunucudan gelen adres `javascript:`
 * olsa bile çalıştırılmaz).
 */
@Injectable({ providedIn: 'root' })
export class SayfaYonlendirici {
  constructor(@Inject(DOCUMENT) private readonly belge: Document) {}

  /** @returns adres geçerliyse ve yönlendirme başladıysa true */
  disAdreseGit(adres: string): boolean {
    let url: URL;
    try {
      url = new URL(adres);
    } catch {
      return false;
    }
    if (!IZINLI_PROTOKOLLER.has(url.protocol)) {
      return false;
    }
    this.belge.location.assign(url.href);
    return true;
  }
}
