import { Pipe, PipeTransform } from '@angular/core';
import { AbstractControl, ValidationErrors } from '@angular/forms';

type MesajUretici = (etiket: string, ayrinti: unknown) => string;

/** Form hata mesajları tek yerde (rules §6.7); anahtarlar Angular'ın ve SinemaDogrulayicilar'ın hata adlarıdır. */
const MESAJLAR: Readonly<Record<string, MesajUretici>> = {
  required: (etiket) => `${etiket} zorunludur.`,
  email: () => 'Geçerli bir e-posta adresi girin.',
  maxlength: (etiket, ayrinti) => `${etiket} en çok ${(ayrinti as { requiredLength: number }).requiredLength} karakter olabilir.`,
  min: (etiket, ayrinti) => `${etiket} en az ${(ayrinti as { min: number }).min} olmalı.`,
  max: (etiket, ayrinti) => `${etiket} en çok ${(ayrinti as { max: number }).max} olabilir.`,
  pattern: (etiket) => `${etiket} biçimi geçersiz.`,
  liste: (etiket, ayrinti) => {
    const sinir = ayrinti as { enFazlaOge: number; ogeEnFazlaKarakter: number };
    return `${etiket} en çok ${sinir.enFazlaOge} öğe olabilir; her biri en çok ${sinir.ogeEnFazlaKarakter} karakter.`;
  },
  parola: () => 'Parola en az 8 karakter olmalı ve çok uzun olmamalı.',
  kisiAdi: (etiket) => `${etiket} yalnızca harf, boşluk, kesme ve tire içerebilir.`,
  fiyat: () => 'Geçerli bir tutar girin (ör. 150 ya da 150,50).',
  telefon: () => 'Geçerli bir cep telefonu girin (ör. 0532 123 45 67).',
  // Sunucunun alan hatası (sunucuHatalariniUygula): mesaj hazır gelir
  sunucu: (_, ayrinti) => String(ayrinti),
};

/**
 * Alanın ilk hatasını Türkçe mesaja çevirir; kullanıcı alana dokunmadıysa boş döner. Saf olmayan (impure) pipe:
 * kontrolün durumu referans değişmeden değiştiği için her değişiklik algılamasında yeniden hesaplanır (işi küçük).
 *
 * Kullanım: `<div class="invalid-feedback">{{ eposta | alanHatasi: 'E-posta' }}</div>`
 */
@Pipe({ name: 'alanHatasi', pure: false })
export class AlanHatasiPipe implements PipeTransform {
  transform(kontrol: AbstractControl, etiket: string): string {
    if (!kontrol.touched || !kontrol.errors) {
      return '';
    }
    return AlanHatasiPipe.ilkMesaj(kontrol.errors, etiket);
  }

  private static ilkMesaj(hatalar: ValidationErrors, etiket: string): string {
    const anahtar = Object.keys(hatalar).find((ad) => ad in MESAJLAR);
    return anahtar ? MESAJLAR[anahtar](etiket, hatalar[anahtar]) : `${etiket} geçersiz.`;
  }
}
