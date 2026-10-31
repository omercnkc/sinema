import { Injectable } from '@angular/core';

/**
 * Sunucu saati (§4.3, §8.4): istemci saati yanlış olabilir, geri sayımlar sunucunun `serverTime` alanına hizalanır.
 * Cevaplardan fark (sunucu − istemci) güncellenir; ekrandaki süre bu farkla hesaplanır. Karar (süre doldu mu) her
 * zaman sunucudadır; bu yalnızca gösterim içindir. Ağ gecikmesi kadar (genelde < 1 sn) sapma kabul edilir.
 */
@Injectable({ providedIn: 'root' })
export class SunucuSaati {
  private farkMs = 0;

  farkiGuncelle(sunucuZamani: string): void {
    const sunucu = Date.parse(sunucuZamani);
    if (Number.isFinite(sunucu)) {
      this.farkMs = sunucu - Date.now();
    }
  }

  /** Sunucuya göre şimdiki an (epoch ms). */
  simdi(): number {
    return Date.now() + this.farkMs;
  }
}
