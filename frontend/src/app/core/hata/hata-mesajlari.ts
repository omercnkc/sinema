/**
 * Kullanıcıya gösterilen hata mesajları tek yerde (rules §6.6, §8.4). Sunucunun "detail" alanı zaten kullanıcıya
 * gösterilebilir Türkçe metindir; bu sözlük yalnızca ön yüzde farklı söylenmesi gereken kodları ve sunucunun cevap
 * veremediği genel durumları (ağ hatası, 5xx) tutar. Teknik ayrıntı (stack, iç kod) asla gösterilmez.
 */
const KOD_MESAJLARI: Readonly<Record<string, string>> = {
  VALIDATION_FAILED: 'Lütfen formdaki hatalı alanları düzeltin.',
  UNAUTHORIZED: 'Bu işlem için giriş yapmalısınız.',
  FORBIDDEN: 'Bu işlem için yetkiniz yok.',
  SEATS_UNAVAILABLE: 'Seçtiğiniz koltuklardan bazıları az önce alındı. Harita güncellendi.',
};

const AG_HATASI = 'Sunucuya ulaşılamıyor. İnternet bağlantınızı kontrol edip tekrar deneyin.';
const SUNUCU_HATASI = 'Beklenmeyen bir hata oluştu. Lütfen biraz sonra tekrar deneyin.';
const HIZMET_YOK = 'Hizmet şu an kullanılamıyor. Lütfen biraz sonra tekrar deneyin.';
const GENEL_HATA = 'İşlem tamamlanamadı.';
const BEKLEME_KODLARI = new Set(['RATE_LIMITED', 'HOLD_COOLDOWN', 'ACCOUNT_LOCKED']);
const SANIYE_PER_DAKIKA = 60;

/**
 * Öncelik: ön yüz sözlüğü → sunucunun detail'i → duruma göre genel mesaj. Bekleme gerektiren kodlarda kalan süre
 * eklenir ("… 2 dakika sonra tekrar deneyin").
 */
export function hataMesaji(durum: number, kod: string, detay: string | undefined, beklemeSaniyesi: number | null): string {
  const temel = KOD_MESAJLARI[kod] ?? detay ?? genelMesaj(durum);
  if (beklemeSaniyesi !== null && BEKLEME_KODLARI.has(kod)) {
    return `${temel} ${sureMetni(beklemeSaniyesi)} sonra tekrar deneyin.`;
  }
  return temel;
}

function genelMesaj(durum: number): string {
  if (durum === 0) {
    return AG_HATASI;
  }
  if (durum === 503 || durum === 502 || durum === 504) {
    return HIZMET_YOK;
  }
  return durum >= 500 ? SUNUCU_HATASI : GENEL_HATA;
}

function sureMetni(saniye: number): string {
  if (saniye < SANIYE_PER_DAKIKA) {
    return `${saniye} saniye`;
  }
  return `${Math.ceil(saniye / SANIYE_PER_DAKIKA)} dakika`;
}
