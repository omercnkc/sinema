package com.sinema.payment.saglayici.mock;

import com.sinema.payment.odeme.entity.OdemeSaglayicisi;
import com.sinema.payment.saglayici.BaslatmaIstegi;
import com.sinema.payment.saglayici.BaslatmaSonucu;
import com.sinema.payment.saglayici.IadeSonucu;
import com.sinema.payment.saglayici.OdemeSaglayiciAdaptoru;
import com.sinema.payment.saglayici.SorguDurumu;
import com.sinema.payment.saglayici.SorguSonucu;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bellek içi sahte sağlayıcı (K49): otomatik testlerde ve iyzico anahtarı olmadan yerelde uçtan uca ödeme denemek
 * için. Davranışı iyzico'ya benzer: sonuç seçilmeden sorgu BEKLIYOR, bilinmeyen token BULUNAMADI döner. Kullanıcı
 * sonucu sahte ödeme sayfasında seçer (MockOdemeSayfasiKontrolcusu).
 *
 * Yalnızca geliştirme içindir: kayıtlar bellektedir, servis yeniden başlarsa kaybolur (callback BULUNAMADI alır).
 */
public class MockOdemeSaglayicisi implements OdemeSaglayiciAdaptoru {

    static final String RED_KODU = "MOCK_CARD_DECLINED";

    private final MockAyarlari ayarlar;
    private final Clock saat;
    private final Map<String, MockOdeme> odemeler = new ConcurrentHashMap<>();
    private final AtomicLong odemeSayaci = new AtomicLong(1_000);

    public MockOdemeSaglayicisi(MockAyarlari ayarlar, Clock saat) {
        this.ayarlar = ayarlar;
        this.saat = saat;
    }

    @Override
    public OdemeSaglayicisi tur() {
        return OdemeSaglayicisi.MOCK;
    }

    @Override
    public BaslatmaSonucu baslat(BaslatmaIstegi istek) {
        String token = UUID.randomUUID().toString();
        odemeler.put(token, new MockOdeme(istek, null, null, false));
        return new BaslatmaSonucu(token, saat.instant().plus(ayarlar.tokenOmru()),
                URI.create(ayarlar.sayfaAdresi() + "/" + token));
    }

    @Override
    public SorguSonucu sonucuSorgula(String token, String konusmaKimligi) {
        MockOdeme odeme = odemeler.get(token);
        if (odeme == null) {
            return sonuc(SorguDurumu.BULUNAMADI, null, null, "MOCK_TOKEN_NOT_FOUND");
        }
        if (odeme.basarili() == null) {
            return sonuc(SorguDurumu.BEKLIYOR, null, odeme.istek(), "MOCK_NOT_PAID");
        }
        return odeme.basarili()
                ? sonuc(SorguDurumu.BASARILI, odeme.saglayiciOdemeKimligi(), odeme.istek(), null)
                : sonuc(SorguDurumu.BASARISIZ, null, odeme.istek(), RED_KODU);
    }

    @Override
    public IadeSonucu iadeEt(String saglayiciOdemeKimligi, BigDecimal tutar, String konusmaKimligi) {
        Optional<Map.Entry<String, MockOdeme>> kayit = odemeler.entrySet().stream()
                .filter(e -> saglayiciOdemeKimligi.equals(e.getValue().saglayiciOdemeKimligi()))
                .findFirst();
        if (kayit.isEmpty()) {
            return IadeSonucu.reddedildi("MOCK_PAYMENT_NOT_FOUND");
        }
        odemeler.computeIfPresent(kayit.get().getKey(), (token, odeme) -> odeme.iadeEdilmis());
        return IadeSonucu.iadeEdildi();
    }

    /** Sahte ödeme sayfasında kullanıcının seçimi. Token bilinmiyorsa ya da zaten sonuçlandıysa false. */
    public boolean sonucuKaydet(String token, boolean basarili) {
        MockOdeme guncel = odemeler.computeIfPresent(token, (t, odeme) -> odeme.basarili() != null ? odeme
                : odeme.sonuclandi(basarili, basarili ? "mock-" + odemeSayaci.incrementAndGet() : null));
        return guncel != null && guncel.basarili() == basarili;
    }

    /** Sahte ödeme sayfasının gösterdiği tutar; token bilinmiyorsa boş. */
    public Optional<BigDecimal> tutar(String token) {
        return Optional.ofNullable(odemeler.get(token)).map(odeme -> odeme.istek().tutar());
    }

    /** Test ve izleme için: iade edildi mi. */
    boolean iadeEdildiMi(String token) {
        return Optional.ofNullable(odemeler.get(token)).map(MockOdeme::iadeEdildi).orElse(false);
    }

    private static SorguSonucu sonuc(SorguDurumu durum, String odemeKimligi, BaslatmaIstegi istek, String hataKodu) {
        String govde = "{\"mock\":true,\"status\":\"" + durum + "\"}";
        if (istek == null) {
            return new SorguSonucu(durum, null, null, null, null, null, hataKodu, govde);
        }
        return new SorguSonucu(durum, odemeKimligi, istek.tutar(), istek.paraBirimi(), istek.sepetKimligi(),
                istek.konusmaKimligi(), hataKodu, govde);
    }

    private record MockOdeme(BaslatmaIstegi istek, Boolean basarili, String saglayiciOdemeKimligi,
            boolean iadeEdildi) {

        MockOdeme sonuclandi(boolean sonuc, String odemeKimligi) {
            return new MockOdeme(istek, sonuc, odemeKimligi, false);
        }

        MockOdeme iadeEdilmis() {
            return new MockOdeme(istek, basarili, saglayiciOdemeKimligi, true);
        }
    }
}
