import { FormGroup } from '@angular/forms';
import { AlanHatasi, UygulamaHatasi } from '../../core/hata/uygulama-hatasi';

const BEKLENMEYEN_HATA = 'Beklenmeyen bir hata oluştu. Lütfen tekrar deneyin.';

/**
 * Sunucunun doğrulama hatalarını (problem+json `errors[]`) aynı adlı form alanlarına yazar; mesaj alanın altında
 * görünür. Kullanıcı alanı değiştirince Angular doğrulayıcıları yeniden çalıştırır ve bu hata kendiliğinden silinir.
 *
 * @returns en az bir alan eşleştiyse true (eşleşmeyen hata için sayfa genel mesaj gösterir)
 */
export function sunucuHatalariniUygula(form: FormGroup, hatalar: readonly AlanHatasi[]): boolean {
  let eslesti = false;
  for (const hata of hatalar) {
    const kontrol = form.get(hata.field);
    if (kontrol) {
      kontrol.setErrors({ ...kontrol.errors, sunucu: hata.message });
      kontrol.markAsTouched();
      eslesti = true;
    }
  }
  return eslesti;
}

/**
 * Form gönderimindeki hatayı yerine koyar: alan hatası olan kodlar ve sunucu doğrulama hataları ilgili alanın altına
 * yazılır; kalan her şey için sayfanın göstereceği genel mesaj döner (alanlara dağıtıldıysa null).
 *
 * @param kodAlanlari alanla ilgili iş hatası kodları (ör. EMAIL_ALREADY_REGISTERED → email)
 */
export function formHatasiniIsle(form: FormGroup, hata: unknown, kodAlanlari: Readonly<Record<string, string>> = {}): string | null {
  if (!(hata instanceof UygulamaHatasi)) {
    return BEKLENMEYEN_HATA;
  }
  const alan = kodAlanlari[hata.kod];
  if (alan && sunucuHatalariniUygula(form, [{ field: alan, message: hata.mesaj }])) {
    return null;
  }
  return sunucuHatalariniUygula(form, hata.alanHatalari) ? null : hata.mesaj;
}
