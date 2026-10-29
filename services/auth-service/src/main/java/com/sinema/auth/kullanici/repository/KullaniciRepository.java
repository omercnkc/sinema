package com.sinema.auth.kullanici.repository;

import com.sinema.auth.kullanici.entity.Kullanici;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KullaniciRepository extends JpaRepository<Kullanici, Long> {

    boolean existsByEmail(String email);

    Optional<Kullanici> findByEmail(String email);
}
