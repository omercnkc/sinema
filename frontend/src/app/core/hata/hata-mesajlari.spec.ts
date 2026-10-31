import { hataMesaji } from './hata-mesajlari';

describe('hataMesaji', () => {
  it('should prefer front-end wording when code has one', () => {
    expect(hataMesaji(409, 'SEATS_UNAVAILABLE', 'sunucu metni', null)).toContain('az önce alındı');
  });

  it('should use server detail when code has no front-end wording', () => {
    expect(hataMesaji(409, 'ACTIVE_RESERVATION_EXISTS', 'Bu seans için bekleyen bir işleminiz var.', null))
      .toBe('Bu seans için bekleyen bir işleminiz var.');
  });

  it('should fall back to general messages by status without technical details', () => {
    expect(hataMesaji(0, 'HTTP_0', undefined, null)).toContain('Sunucuya ulaşılamıyor');
    expect(hataMesaji(503, 'HTTP_503', undefined, null)).toContain('Hizmet şu an kullanılamıyor');
    expect(hataMesaji(500, 'HTTP_500', undefined, null)).toContain('Beklenmeyen bir hata');
    expect(hataMesaji(404, 'HTTP_404', undefined, null)).toBe('İşlem tamamlanamadı.');
  });

  it('should append remaining wait time for rate limit codes', () => {
    expect(hataMesaji(429, 'HOLD_COOLDOWN', 'Biraz bekleyin.', 480)).toBe('Biraz bekleyin. 8 dakika sonra tekrar deneyin.');
    expect(hataMesaji(429, 'RATE_LIMITED', 'Çok fazla istek.', 12)).toBe('Çok fazla istek. 12 saniye sonra tekrar deneyin.');
  });

  it('should not append wait time for other codes', () => {
    expect(hataMesaji(409, 'PAYMENT_IN_PROGRESS', 'Ödeme sürüyor.', 30)).toBe('Ödeme sürüyor.');
  });
});
