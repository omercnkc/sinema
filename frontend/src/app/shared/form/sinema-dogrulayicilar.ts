import { AbstractControl, ValidationErrors } from '@angular/forms';

// Kurallar auth-service ile aynı (KayitIstegi, GecerliParola, TelefonNumarasi); asıl güvence sunucudadır (rules §7)
const PAROLA_EN_AZ_KARAKTER = 8;
/** BCrypt yalnızca ilk 72 baytı kullanır; sınır karakter değil UTF-8 bayttır (Türkçe harfler 2 bayt). */
const PAROLA_EN_FAZLA_BAYT = 72;
export const KISI_ADI_EN_FAZLA_KARAKTER = 50;
export const EPOSTA_EN_FAZLA_KARAKTER = 255;
// Harfle başlar; harf (Türkçe dahil), boşluk, kesme, tire. \p{L} için 'u' bayrağı gerekir.
const KISI_ADI_DESENI = new RegExp("^\\s*\\p{L}[\\p{L} '\\-]*\\s*$", 'u');
// Ayraçlar atıldıktan sonra: isteğe bağlı önek (+90, 0090, 90, 0) ve 5 ile başlayan 10 hane (TR cep)
const TELEFON_AYRACLARI = /[\s\-().]/g;
const TR_CEP_DESENI = /^(?:\+90|0090|90|0)?5\d{9}$/;

// Para: en çok 8 tam, 2 ondalık basamak (sunucuda @Digits(integer = 8, fraction = 2)); ondalık ayracı virgül ya da nokta
const FIYAT_DESENI = /^\d{1,8}([.,]\d{1,2})?$/;

/** Kullanıcının yazdığı fiyatı ("150,5") sunucunun beklediği biçime ("150.5") çevirir; para string gider (§7.2). */
export function fiyatiNormalizeEt(deger: unknown): string {
  return String(deger ?? '').trim().replace(',', '.');
}

/** Boş değeri `Validators.required`'a bırakır: alan zorunlu değilse boş geçerlidir. */
function bosMu(kontrol: AbstractControl): boolean {
  return kontrol.value === null || kontrol.value === '';
}

/** Projeye özel, yeniden kullanılan form doğrulayıcıları (rules §6.7). Hata anahtarları AlanHatasiPipe'ta Türkçeleşir. */
export class SinemaDogrulayicilar {
  static parola(kontrol: AbstractControl): ValidationErrors | null {
    if (bosMu(kontrol)) {
      return null;
    }
    const parola = String(kontrol.value);
    const gecerli = parola.length >= PAROLA_EN_AZ_KARAKTER && new TextEncoder().encode(parola).length <= PAROLA_EN_FAZLA_BAYT;
    return gecerli ? null : { parola: true };
  }

  static kisiAdi(kontrol: AbstractControl): ValidationErrors | null {
    return bosMu(kontrol) || KISI_ADI_DESENI.test(String(kontrol.value)) ? null : { kisiAdi: true };
  }

  static telefon(kontrol: AbstractControl): ValidationErrors | null {
    if (bosMu(kontrol)) {
      return null;
    }
    const sade = String(kontrol.value).trim().replace(TELEFON_AYRACLARI, '');
    return TR_CEP_DESENI.test(sade) ? null : { telefon: true };
  }

  static fiyat(kontrol: AbstractControl): ValidationErrors | null {
    return bosMu(kontrol) || FIYAT_DESENI.test(String(kontrol.value).trim()) ? null : { fiyat: true };
  }
}
