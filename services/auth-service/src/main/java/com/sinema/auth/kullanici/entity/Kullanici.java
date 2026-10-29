package com.sinema.auth.kullanici.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA ister; uygulama kodu fabrika metodunu kullanır
public class Kullanici {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String parolaOzeti;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Rol rol;

    @Column(name = "failed_login_count", nullable = false)
    private int basarisizGirisSayisi;

    @Column(name = "locked_until")
    private Instant kilitBitisi;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant olusturulmaZamani;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant guncellenmeZamani;

    private Kullanici(String email, String parolaOzeti, Rol rol) {
        this.email = email;
        this.parolaOzeti = parolaOzeti;
        this.rol = rol;
    }

    /** Yeni kullanıcı; e-postanın normalize, parolanın özetlenmiş olması çağıranın sorumluluğudur. */
    public static Kullanici olustur(String normalizeEmail, String parolaOzeti, Rol rol) {
        return new Kullanici(normalizeEmail, parolaOzeti, rol);
    }

    // Kimlik id'dir; kaydedilmemiş (id'siz) iki nesne eşit sayılmaz. hashCode sabittir, çünkü id kayıtta değişir.
    @Override
    public boolean equals(Object diger) {
        if (this == diger) {
            return true;
        }
        return diger instanceof Kullanici kullanici && id != null && id.equals(kullanici.id);
    }

    @Override
    public int hashCode() {
        return Kullanici.class.hashCode();
    }
}
