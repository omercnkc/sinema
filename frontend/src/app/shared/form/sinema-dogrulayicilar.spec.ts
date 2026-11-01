import { FormControl } from '@angular/forms';
import { fiyatiNormalizeEt, SinemaDogrulayicilar } from './sinema-dogrulayicilar';

describe('SinemaDogrulayicilar', () => {
  function gecerli(dogrulayici: (kontrol: FormControl) => unknown, deger: string): boolean {
    return dogrulayici(new FormControl(deger)) === null;
  }

  it('should accept Turkish mobile numbers in the formats the server accepts', () => {
    ['0532 123 45 67', '5321234567', '+90 532 123 4567', '(0532) 123-45-67', '00905321234567', ''].forEach((numara) =>
      expect(gecerli(SinemaDogrulayicilar.telefon, numara)).withContext(numara).toBeTrue());
    ['0212 123 45 67', '532123456', '+1 532 123 4567', 'telefon'].forEach((numara) =>
      expect(gecerli(SinemaDogrulayicilar.telefon, numara)).withContext(numara).toBeFalse());
  });

  it('should allow letters including Turkish, spaces, apostrophes and hyphens in names', () => {
    ['Ayşe', 'Çağrı Öztürk', "O'Neil", 'Ali-Rıza'].forEach((ad) =>
      expect(gecerli(SinemaDogrulayicilar.kisiAdi, ad)).withContext(ad).toBeTrue());
    ['-Ali', 'Ali3', 'Ali!'].forEach((ad) => expect(gecerli(SinemaDogrulayicilar.kisiAdi, ad)).withContext(ad).toBeFalse());
  });

  it('should limit password by 8 characters minimum and 72 UTF-8 bytes maximum', () => {
    expect(gecerli(SinemaDogrulayicilar.parola, 'kisa123')).toBeFalse();
    expect(gecerli(SinemaDogrulayicilar.parola, 'Parola12')).toBeTrue();
    expect(gecerli(SinemaDogrulayicilar.parola, 'a'.repeat(72))).toBeTrue();
    // 37 x "ş" = 74 bayt (karakter sayısı 72'nin altında olsa da BCrypt sınırını aşar)
    expect(gecerli(SinemaDogrulayicilar.parola, 'ş'.repeat(37))).toBeFalse();
  });

  it('should accept prices with comma or dot and at most 2 decimals', () => {
    ['150', '150,5', '150.50', '0', '12345678.99', ''].forEach((fiyat) =>
      expect(gecerli(SinemaDogrulayicilar.fiyat, fiyat)).withContext(fiyat).toBeTrue());
    ['-1', '150,555', '123456789', '1.2.3', 'yüz'].forEach((fiyat) =>
      expect(gecerli(SinemaDogrulayicilar.fiyat, fiyat)).withContext(fiyat).toBeFalse());
    expect(fiyatiNormalizeEt(' 150,5 ')).toBe('150.5');
  });
});
