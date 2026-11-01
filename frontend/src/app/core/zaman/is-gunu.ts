import { formatDate } from '@angular/common';
import { IS_SAAT_DILIMI } from './is-saat-dilimi';

const GUN_MS = 24 * 60 * 60 * 1000;
/** Tarih anahtarı biçimi; sunucunun `?date=` parametresiyle aynı (ISO tarih). Yerel ayardan bağımsız. */
const ANAHTAR_BICIMI = 'yyyy-MM-dd';
const ANAHTAR_YERELI = 'en-US';

export interface IsGunu {
  /** `YYYY-MM-DD` (İstanbul günü), sunucuya `?date=` olarak gider */
  readonly tarih: string;
  /** Ekranda: "Bugün", "Yarın" ya da "Pzt 2 Kas" */
  readonly etiket: string;
}

/** Verilen anın İstanbul'daki günü (`YYYY-MM-DD`); tarayıcının saat diliminden bağımsız. */
export function istanbulGunu(an: Date): string {
  return formatDate(an, ANAHTAR_BICIMI, ANAHTAR_YERELI, IS_SAAT_DILIMI);
}

/**
 * İstanbul'daki gün ve saati (`YYYY-MM-DD`, `HH:mm`) UTC ISO anına çevirir (sunucuya `startsAt` olarak gider).
 * Yaz saati olmadığı için sabit ofset yeterlidir; tarayıcının saat diliminden bağımsızdır.
 */
export function istanbulAni(tarih: string, saat: string): string {
  const ofset = IS_SAAT_DILIMI.replace(/(\d{2})(\d{2})$/, '$1:$2');
  return new Date(`${tarih}T${saat}:00${ofset}`).toISOString();
}

/** `YYYY-MM-DD` tarihine gün ekler/çıkarır (takvim aritmetiği UTC'de; saat dilimi kayması olmaz). */
export function gunEkle(tarih: string, gunSayisi: number): string {
  return new Date(Date.parse(`${tarih}T00:00:00Z`) + gunSayisi * GUN_MS).toISOString().slice(0, 10);
}

/**
 * Bugünden başlayan `adet` İstanbul günü (seans gün seçicisi). İstanbul'da yaz saati olmadığı için gün eklemek 24
 * saat eklemektir. `yerel` ekrandaki gün/ay adları içindir (uygulamada 'tr').
 */
export function istanbulGunleri(simdi: Date, adet: number, yerel: string): IsGunu[] {
  return Array.from({ length: adet }, (_, sira) => {
    const an = new Date(simdi.getTime() + sira * GUN_MS);
    return { tarih: istanbulGunu(an), etiket: gunEtiketi(an, sira, yerel) };
  });
}

function gunEtiketi(an: Date, sira: number, yerel: string): string {
  if (sira === 0) {
    return 'Bugün';
  }
  return sira === 1 ? 'Yarın' : formatDate(an, 'EEE d MMM', yerel, IS_SAAT_DILIMI);
}
