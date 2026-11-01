/** Admin şube listesindeki salon: fiyat string ("120.00", §7.2), düzen yayımlanmamışsa `activeLayoutId` boş. */
export interface YonetimSalon {
  readonly id: number;
  readonly cinemaId: number;
  readonly name: string;
  readonly defaultTicketPrice: string;
  readonly activeLayoutId: number | null;
}

/** `GET /admin/catalog/cinemas`: şubeler salonlarıyla birlikte. */
export interface YonetimSube {
  readonly id: number;
  readonly name: string;
  readonly city: string;
  readonly address: string;
  readonly halls: readonly YonetimSalon[];
}

/** Şube oluşturma ve güncelleme gövdesi (aynı alanlar). */
export interface SubeIstegi {
  readonly name: string;
  readonly city: string;
  readonly address: string;
}

/** Salon güncelleme gövdesi. Şube sonradan değişmez; fiyat değişikliği yalnızca yeni seanslara kopyalanır (K23). */
export interface SalonIstegi {
  readonly name: string;
  readonly defaultTicketPrice: string;
}
