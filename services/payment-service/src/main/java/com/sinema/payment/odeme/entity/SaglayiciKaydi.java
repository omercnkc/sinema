package com.sinema.payment.odeme.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Sağlayıcıyla yaşanan her olayın ham kaydı (payment_callback_logs): denetim ve sorun giderme için. Değişmez;
 * 90 gün sonra silinir (K43). Ham gövde kaydedilmeden önce maskelenmiş olmalı: kart verisi tutulmaz.
 * Bilinmeyen token'la gelen callback da kaydedilir (ödeme yoktur, paymentId boş): sahte istek izi.
 */
@Entity
@Table(name = "payment_callback_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaglayiciKaydi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", updatable = false)
    private Long odemeId;

    @Column(name = "provider_token", updatable = false)
    private String saglayiciTokeni;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_source", nullable = false, length = 20, updatable = false)
    private OlayKaynagi kaynak;

    @Column(name = "retrieve_status", length = 30, updatable = false)
    private String sonucDurumu;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", updatable = false)
    private String hamGovde;

    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant alinmaZamani;

    /** maskeliGovde: JSON metni, hassas alanları zaten maskelenmiş (çağıranın sorumluluğu). */
    public static SaglayiciKaydi olustur(Long odemeId, String saglayiciTokeni, OlayKaynagi kaynak, String sonucDurumu,
            String maskeliGovde, Instant alinmaZamani) {
        SaglayiciKaydi kayit = new SaglayiciKaydi();
        kayit.odemeId = odemeId;
        kayit.saglayiciTokeni = saglayiciTokeni;
        kayit.kaynak = kaynak;
        kayit.sonucDurumu = sonucDurumu;
        kayit.hamGovde = maskeliGovde;
        kayit.alinmaZamani = alinmaZamani;
        return kayit;
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof SaglayiciKaydi kayit && id != null && id.equals(kayit.id);
    }

    @Override
    public int hashCode() {
        return SaglayiciKaydi.class.hashCode();
    }
}
