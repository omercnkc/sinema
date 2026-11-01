import { environment } from '../../../../environments/environment';

/*
 * Yönetim uçlarının taban adresleri (gateway'de ADMIN rotaları, planlama §3.5 ve §7.7). Admin servisleri isteklerini
 * `sessizBaglam()` ile gönderir: hata genel bildirim yerine ilgili sayfada (formda ya da listede) gösterilir.
 */
export const ADMIN_AUTH_ADRESI = `${environment.apiTabanAdresi}/admin/auth`;
export const ADMIN_KATALOG_ADRESI = `${environment.apiTabanAdresi}/admin/catalog`;
export const ADMIN_REZERVASYON_ADRESI = `${environment.apiTabanAdresi}/admin/bookings`;
