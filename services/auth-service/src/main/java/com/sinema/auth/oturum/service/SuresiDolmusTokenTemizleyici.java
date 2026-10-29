package com.sinema.auth.oturum.service;

import com.sinema.auth.oturum.repository.YenilemeTokeniRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Süresi dolmuş refresh token kayıtlarını siler. Yalnızca tablo büyümesin diye vardır:
 * doğruluk bu job'a bağlı değildir, yenilemede süre zaten her seferinde kontrol edilir.
 */
@Component
@RequiredArgsConstructor
public class SuresiDolmusTokenTemizleyici {

    private static final Logger log = LoggerFactory.getLogger(SuresiDolmusTokenTemizleyici.class);

    private final YenilemeTokeniRepository tokenRepository;
    private final Clock saat;

    @Scheduled(cron = "${sinema.auth.yenileme.temizlik-zamani}", zone = "UTC")
    @Transactional
    public void temizle() {
        int silinen = tokenRepository.suresiDolanlariSil(saat.instant());
        log.info("Süresi dolmuş {} refresh token silindi", silinen);
    }
}
