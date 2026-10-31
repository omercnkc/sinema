/**
 * Ekranda gösterilen saat dilimi (§2.4-9): API UTC döner, ekranda Europe/Istanbul. Türkiye 2016'dan beri sabit UTC+3
 * (yaz saati yok). Angular 13'ün DatePipe'ı IANA adını ("Europe/Istanbul") değil yalnızca ofseti kabul ettiği için
 * ofset kullanılır (M5 alt görev 1'de testle doğrulandı, bkz. is-saat-dilimi.spec.ts).
 */
export const IS_SAAT_DILIMI = '+0300';
