package com.sinema.payment.odeme.entity;

/**
 * Ödeme durum makinesi (planlama §2.3, §4.8):
 * INITIATED → PENDING (ödeme sayfası açıldı) → SUCCEEDED | FAILED; SUCCEEDED ama rezervasyon onaylanamadıysa
 * (süre doldu) REFUND_REQUIRED → REFUNDED. Bu iade kullanıcı iptali değil, sistem düzeltmesidir (K6).
 */
public enum OdemeDurumu {
    INITIATED,
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUND_REQUIRED,
    REFUNDED
}
