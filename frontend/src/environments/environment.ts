// Geliştirme ortamı (ng serve). Değerler koda gömülmez; servisler buradan okur (rules §6.6).
export const environment = {
  production: false,
  /** Gateway (ayrı origin, K37); tarayıcı API'ye doğrudan gider, proxy yok. */
  apiTabanAdresi: 'http://localhost:8080/api/v1',
  /** TMDB görsellerinin taban adresi; yalnızca posterPath saklanır, afiş hotlink edilir (K41). */
  tmdbGorselTabanAdresi: 'https://image.tmdb.org/t/p',
  /** Koltuk doluluğunun yenilenme aralığı (K33: 5 sn, yapılandırılabilir). */
  dolulukYenilemeMs: 5000,
  /** Üst menüdeki bildirim zilinin okunmamış sayısını yenileme aralığı (sayfa değişiminde ayrıca yenilenir). */
  bildirimYenilemeMs: 60000,
};
