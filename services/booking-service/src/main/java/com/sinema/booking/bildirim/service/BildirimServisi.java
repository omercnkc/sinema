package com.sinema.booking.bildirim.service;

import com.sinema.booking.bildirim.dto.BildirimCevabi;
import com.sinema.booking.bildirim.dto.OkunmamisSayisi;
import com.sinema.booking.bildirim.entity.Bildirim;
import com.sinema.booking.bildirim.repository.BildirimRepository;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import com.sinema.common.sayfa.SayfaCevabi;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Kullanıcının kendi bildirimleri (K35). Başkasının bildirimi bulunamamış sayılır (varlığı sızdırılmaz, §7.2). */
@Service
@RequiredArgsConstructor
public class BildirimServisi {

    public static final String BILDIRIM_YOK_KODU = "NOTIFICATION_NOT_FOUND";

    private final BildirimRepository bildirimRepository;
    private final Clock saat;

    @Transactional(readOnly = true)
    public SayfaCevabi<BildirimCevabi> listele(Long kullaniciId, int sayfa, int boyut) {
        return SayfaCevabi.from(bildirimRepository.kullanicininBildirimleri(kullaniciId, PageRequest.of(sayfa, boyut))
                .map(BildirimServisi::cevap));
    }

    @Transactional(readOnly = true)
    public OkunmamisSayisi okunmamisSayisi(Long kullaniciId) {
        return new OkunmamisSayisi(bildirimRepository.countByKullaniciIdAndOkunmaZamaniIsNull(kullaniciId));
    }

    @Transactional
    public void okundu(Long kullaniciId, Long bildirimId) {
        bildirimRepository.findByIdAndKullaniciId(bildirimId, kullaniciId)
                .orElseThrow(() -> new KaynakBulunamadiIstisnasi(BILDIRIM_YOK_KODU, "Bildirim bulunamadı."))
                .okundu(saat.instant());
    }

    private static BildirimCevabi cevap(Bildirim bildirim) {
        return new BildirimCevabi(bildirim.getId(), bildirim.getTur(), bildirim.getRezervasyonId(), bildirim.getMesaj(),
                bildirim.getOkunmaZamani() != null, bildirim.getOlusturulmaZamani());
    }
}
