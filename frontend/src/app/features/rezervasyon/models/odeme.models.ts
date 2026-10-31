/** payment-service ödeme durumları. REFUND_REQUIRED/REFUNDED: ödeme alındı ama bilet verilemedi (geç geldi), iade. */
export type OdemeDurumu = 'INITIATED' | 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'REFUND_REQUIRED' | 'REFUNDED';

/** `POST /payments` ve `GET /payments/by-reservation/{id}` cevabı. Tutar sunucudan (istemci tutar göndermez). */
export interface Odeme {
  readonly paymentId: number;
  readonly reservationId: number;
  readonly status: OdemeDurumu;
  readonly amount: string;
  readonly paymentPageUrl: string | null;
}
