package com.sinema.auth.oturum.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Refresh token kaydı. Aynı girişten doğan tüm token'lar bir "aile"dir (family_id):
 * her yenilemede eski token iptal edilir, yerine geçenin id'si yazılır (rotasyon zinciri).
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class YenilemeTokeni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kullanici'ya @ManyToOne yerine id: token işlemleri kullanıcı nesnesine ihtiyaç duymaz, gereksiz yükleme olmaz
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long kullaniciId;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64, columnDefinition = "CHAR(64)")
    private String tokenOzeti;

    @Column(name = "family_id", nullable = false, updatable = false, length = 36, columnDefinition = "CHAR(36)")
    private String aileKimligi;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant sonGecerlilik;

    @Column(name = "revoked_at")
    private Instant iptalZamani;

    @Column(name = "replaced_by_id")
    private Long yerineGecenId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    private YenilemeTokeni(Long kullaniciId, String tokenOzeti, String aileKimligi, Instant olusturulma,
            Instant sonGecerlilik) {
        this.kullaniciId = kullaniciId;
        this.tokenOzeti = tokenOzeti;
        this.aileKimligi = aileKimligi;
        this.olusturulmaZamani = olusturulma;
        this.sonGecerlilik = sonGecerlilik;
    }

    public static YenilemeTokeni olustur(Long kullaniciId, String tokenOzeti, String aileKimligi, Instant simdi,
            Instant sonGecerlilik) {
        return new YenilemeTokeni(kullaniciId, tokenOzeti, aileKimligi, simdi, sonGecerlilik);
    }

    /** Rotasyon: bu token artık geçersiz, yerine yenisi geçti. */
    public void yenisiyleDegistir(Instant simdi, Long yeniTokenId) {
        this.iptalZamani = simdi;
        this.yerineGecenId = yeniTokenId;
    }

    public boolean iptalEdilmisMi() {
        return iptalZamani != null;
    }

    public boolean suresiDolmusMu(Instant simdi) {
        return !sonGecerlilik.isAfter(simdi);
    }

    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof YenilemeTokeni token && id != null && id.equals(token.id);
    }

    @Override
    public int hashCode() {
        return YenilemeTokeni.class.hashCode();
    }
}
