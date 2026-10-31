package com.sinema.payment.odeme.service;

import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.KaynakBulunamadiIstisnasi;
import com.sinema.payment.config.OdemeAyarlari;
import com.sinema.payment.kullanici.istemci.AliciBilgisi;
import com.sinema.payment.kullanici.istemci.KullaniciIstemcisi;
import com.sinema.payment.odeme.dto.OdemeCevabi;
import com.sinema.payment.odeme.entity.Odeme;
import com.sinema.payment.odeme.entity.OdemeDurumu;
import com.sinema.payment.odeme.repository.OdemeRepository;
import com.sinema.payment.rezervasyon.istemci.RezervasyonIstemcisi;
import com.sinema.payment.rezervasyon.istemci.RezervasyonOdemeBilgisi;
import com.sinema.payment.saglayici.BaslatmaIstegi;
import com.sinema.payment.saglayici.BaslatmaSonucu;
import com.sinema.payment.saglayici.OdemeSaglayiciAdaptoru;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Ödeme başlatma (planlama §7.6, §4.8, K32, K48): tutar ve sahiplik booking'den, alıcı bilgisi auth'tan gelir;
 * istemci yalnızca rezervasyon id'si gönderir. Rezervasyon başına tek ödeme: tekrar gelen istek, sayfa hâlâ
 * geçerliyse aynı ödemeyi döner.
 *
 * Bilerek @Transactional değil: booking, auth ve sağlayıcıya HTTP çağrıları DB transaction'ı açıkken yapılmasın.
 * Sağlayıcı başarısız olursa kayıt INITIATED kalır; aynı istek tekrar geldiğinde o kayıtla yeniden denenir.
 */
@Service
@RequiredArgsConstructor
public class OdemeBaslatmaServisi {

    public static final String REZERVASYON_YOK_KODU = "RESERVATION_NOT_FOUND";
    public static final String ODENEMEZ_KODU = "RESERVATION_NOT_PAYABLE";
    public static final String TAMAMLANMIS_KODU = "PAYMENT_ALREADY_COMPLETED";
    public static final String KAPANMIS_KODU = "PAYMENT_CLOSED";
    public static final String BASLATILIYOR_KODU = "PAYMENT_STARTING";
    private static final String KALEM_ADI = "Sinema bileti, rezervasyon #";

    private final RezervasyonIstemcisi rezervasyonIstemcisi;
    private final KullaniciIstemcisi kullaniciIstemcisi;
    private final OdemeSaglayiciAdaptoru saglayici;
    private final OdemeRepository odemeRepository;
    private final OdemeAyarlari ayarlar;
    private final Clock saat;

    public Sonuc baslat(Long kullaniciId, Long rezervasyonId) {
        RezervasyonOdemeBilgisi rezervasyon = sahibininRezervasyonu(kullaniciId, rezervasyonId);
        Optional<Odeme> mevcut = odemeRepository.findByRezervasyonId(rezervasyonId);
        Optional<Odeme> gecerli = mevcutOdemeyiDegerlendir(mevcut, rezervasyon);
        if (gecerli.isPresent()) {
            return new Sonuc(cevap(gecerli.get()), false);
        }
        rezervasyonIstemcisi.odemePenceresiniUzat(rezervasyonId); // K32: bir kez +2 dk; tekrarı zararsız
        AliciBilgisi alici = kullaniciIstemcisi.aliciBilgisi(kullaniciId);
        Odeme odeme = mevcut.orElseGet(() -> kaydiAc(rezervasyon));
        BaslatmaSonucu baslatma = saglayici.baslat(baslatmaIstegi(odeme, alici));
        odeme.saglayicidaBaslatildi(baslatma.token(), baslatma.tokenBitisi(), baslatma.odemeSayfasi().toString());
        return new Sonuc(cevap(odemeRepository.save(odeme)), true);
    }

    /** GET by-reservation: yalnızca ödemenin sahibi görür; başkasınınki 404 (§7.2). */
    public OdemeCevabi rezervasyonOdemesi(Long kullaniciId, Long rezervasyonId) {
        return odemeRepository.findByRezervasyonId(rezervasyonId)
                .filter(odeme -> odeme.getKullaniciId().equals(kullaniciId))
                .map(this::cevap)
                .orElseThrow(() -> new KaynakBulunamadiIstisnasi("PAYMENT_NOT_FOUND", "Ödeme bulunamadı."));
    }

    /** Başkasının rezervasyonu "yok" gibi görünür: varlığı sızdırılmaz. */
    private RezervasyonOdemeBilgisi sahibininRezervasyonu(Long kullaniciId, Long rezervasyonId) {
        RezervasyonOdemeBilgisi rezervasyon = rezervasyonIstemcisi.odemeBilgisi(rezervasyonId);
        if (!kullaniciId.equals(rezervasyon.userId())) {
            throw new KaynakBulunamadiIstisnasi(REZERVASYON_YOK_KODU, "Rezervasyon bulunamadı.");
        }
        return rezervasyon;
    }

    /**
     * Sayfası hâlâ geçerli bir ödeme varsa onu döner (idempotent). Tamamlanmış ya da kapanmış ödeme ve ödenemez
     * rezervasyon 409 olur. INITIATED (sağlayıcı önceki denemede başarısız) yeniden denenmek üzere boş döner.
     */
    private Optional<Odeme> mevcutOdemeyiDegerlendir(Optional<Odeme> mevcut, RezervasyonOdemeBilgisi rezervasyon) {
        if (mevcut.isPresent() && mevcut.get().getDurum() == OdemeDurumu.SUCCEEDED) {
            throw new CakismaIstisnasi(TAMAMLANMIS_KODU, "Bu rezervasyonun ödemesi zaten alındı.");
        }
        if (!rezervasyon.odemeBekliyor() || !rezervasyon.expiresAt().isAfter(rezervasyon.serverTime())) {
            throw new CakismaIstisnasi(ODENEMEZ_KODU, "Bu rezervasyonun süresi dolmuş ya da kapanmış; koltukları"
                    + " yeniden seçin.");
        }
        Instant simdi = saat.instant();
        if (mevcut.isEmpty() || mevcut.get().getDurum() == OdemeDurumu.INITIATED) {
            return Optional.empty();
        }
        if (mevcut.get().sayfaGecerliMi(simdi)) {
            return mevcut;
        }
        throw new CakismaIstisnasi(KAPANMIS_KODU, "Bu rezervasyonun ödeme sayfası kapandı; koltukları yeniden seçin.");
    }

    /**
     * Çift tıklama: iki istek aynı anda kayıt açmaya çalışırsa UNIQUE(reservation_id) ikincisini reddeder;
     * o istek, diğerinin işi bitince tekrar denesin diye 409 alır (sayfa hazırsa tekrar istek aynısını döner).
     */
    private Odeme kaydiAc(RezervasyonOdemeBilgisi rezervasyon) {
        try {
            return odemeRepository.saveAndFlush(Odeme.olustur(rezervasyon.reservationId(), rezervasyon.userId(),
                    saglayici.tur(), rezervasyon.totalAmount(), UUID.randomUUID().toString()));
        } catch (DataIntegrityViolationException e) {
            throw new CakismaIstisnasi(BASLATILIYOR_KODU, "Ödeme hazırlanıyor, lütfen birkaç saniye sonra tekrar deneyin.");
        }
    }

    private BaslatmaIstegi baslatmaIstegi(Odeme odeme, AliciBilgisi alici) {
        return new BaslatmaIstegi(String.valueOf(odeme.getRezervasyonId()), odeme.getKonusmaKimligi(),
                odeme.getTutar(), odeme.getParaBirimi(), KALEM_ADI + odeme.getRezervasyonId(), alici(alici),
                ayarlar.callbackAdresi());
    }

    /** K48: gerçek ad/soyad/e-posta/telefon; boş olan (admin, eski hesap) ve toplanmayan alanlar sabit değerden. */
    private BaslatmaIstegi.Alici alici(AliciBilgisi bilgi) {
        OdemeAyarlari.SabitAliciBilgileri sabit = ayarlar.alici();
        return new BaslatmaIstegi.Alici(String.valueOf(bilgi.userId()),
                varsayilanli(bilgi.firstName(), sabit.yedekAd()), varsayilanli(bilgi.lastName(), sabit.yedekSoyad()),
                bilgi.email(), varsayilanli(bilgi.phone(), sabit.yedekTelefon()),
                sabit.kimlikNo(), sabit.adres(), sabit.sehir(), sabit.ulke());
    }

    private static String varsayilanli(String deger, String yedek) {
        return StringUtils.hasText(deger) ? deger : yedek;
    }

    private OdemeCevabi cevap(Odeme odeme) {
        String sayfa = odeme.sayfaGecerliMi(saat.instant()) ? odeme.getOdemeSayfasi() : null;
        return new OdemeCevabi(odeme.getId(), odeme.getRezervasyonId(), odeme.getDurum(), odeme.getTutar(), sayfa);
    }

    /** yeni: bu istekle mi başlatıldı (201) yoksa mevcut ödeme mi döndü (200). */
    public record Sonuc(OdemeCevabi cevap, boolean yeni) {
    }
}
