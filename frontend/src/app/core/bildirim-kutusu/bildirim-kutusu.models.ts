/** Uygulama içi bildirim türleri (booking BildirimTuru). */
export type UygulamaBildirimiTuru = 'SEAT_CHANGED';

/** `GET /bookings/notifications` öğesi; `reservationId` varsa ilgili bilete bağlantı verilir. */
export interface UygulamaBildirimi {
  readonly id: number;
  readonly type: UygulamaBildirimiTuru;
  readonly reservationId: number | null;
  readonly message: string;
  readonly read: boolean;
  readonly createdAt: string;
}
