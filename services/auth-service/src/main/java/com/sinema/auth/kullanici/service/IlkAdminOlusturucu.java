package com.sinema.auth.kullanici.service;

import com.sinema.auth.config.KimlikAyarlari;
import com.sinema.auth.kullanici.dto.KayitIstegi;
import com.sinema.auth.kullanici.entity.Rol;
import com.sinema.auth.kullanici.repository.KullaniciRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Açılışta, sistemde hiç ADMIN yoksa .env'deki ADMIN_EMAIL / ADMIN_PASSWORD ile ilk admini oluşturur (K36).
 * Herkese açık kayıt hep USER ürettiği için ilk admin başka türlü oluşturulamaz.
 * Admin varsa hiçbir şey yapmaz: .env değişse bile mevcut admin ezilmez, parolası değişmez.
 */
@Component
@RequiredArgsConstructor
public class IlkAdminOlusturucu implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IlkAdminOlusturucu.class);

    private final KimlikAyarlari ayarlar;
    private final KullaniciRepository kullaniciRepository;
    private final KullaniciKayitServisi kayitServisi;
    private final Validator dogrulayici;

    @Override
    public void run(ApplicationArguments argumanlar) {
        KimlikAyarlari.IlkAdmin ilkAdmin = ayarlar.ilkAdmin();
        if (ilkAdmin == null || !StringUtils.hasText(ilkAdmin.email())) {
            log.info("İlk admin tanımlı değil (ADMIN_EMAIL boş); atlanıyor");
            return;
        }
        if (kullaniciRepository.existsByRol(Rol.ADMIN)) {
            return;
        }
        KayitIstegi istek = new KayitIstegi(ilkAdmin.email(), ilkAdmin.parola());
        gecerliDegilseDurdur(istek);
        try {
            kayitServisi.adminEkle(istek);
            log.info("İlk admin oluşturuldu");
        } catch (CakismaIstisnasi e) {
            // Aynı e-postayla bir USER hesabı varsa sessizce admin'e yükseltmek yetki yükseltme açığı olurdu
            log.warn("ADMIN_EMAIL ile kayıtlı bir kullanıcı hesabı var; admin oluşturulmadı, yetkisi yükseltilmedi");
        }
    }

    // Zayıf/hatalı admin bilgisiyle servisin açılması yerine açık bir hata ile durmak tercih edilir (fail fast)
    private void gecerliDegilseDurdur(KayitIstegi istek) {
        Set<ConstraintViolation<KayitIstegi>> ihlaller = dogrulayici.validate(istek);
        if (!ihlaller.isEmpty()) {
            String nedenler = ihlaller.stream().map(ConstraintViolation::getMessage).collect(Collectors.joining(" "));
            throw new IllegalStateException("ADMIN_EMAIL / ADMIN_PASSWORD geçersiz: " + nedenler);
        }
    }
}
