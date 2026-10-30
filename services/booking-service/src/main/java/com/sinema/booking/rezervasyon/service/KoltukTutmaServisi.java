package com.sinema.booking.rezervasyon.service;

import com.sinema.booking.config.RezervasyonAyarlari;
import com.sinema.booking.doluluk.service.DolulukOnbellegi;
import com.sinema.booking.katalog.istemci.KatalogIstemcisi;
import com.sinema.booking.katalog.istemci.KatalogSeansBilgisi;
import com.sinema.booking.katalog.istemci.KoltukCozumlemeIstegi;
import com.sinema.booking.katalog.istemci.KoltukCozumlemeSonucu;
import com.sinema.booking.koltuk.repository.KoltukKilitDeposu;
import com.sinema.booking.rezervasyon.dto.KoltukTutmaIstegi;
import com.sinema.booking.rezervasyon.dto.RezervasyonCevabi;
import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonDurumu;
import com.sinema.booking.rezervasyon.entity.RezervasyonKoltugu;
import com.sinema.booking.rezervasyon.mapper.RezervasyonMapper;
import com.sinema.booking.rezervasyon.repository.RezervasyonRepository;
import com.sinema.common.hata.CakismaIstisnasi;
import com.sinema.common.hata.GecersizIstekIstisnasi;
import com.sinema.common.hata.IsKuraliIstisnasi;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

/**
 * Koltuk tutma akışı (planlama §4.3): ucuz kontroller (sınır, aktif işlem, K29) → catalog (seans, koltuklar) → tek transaction'da tutma.
 * Kontroller "fail fast" içindir; asıl güvence veritabanıdır (K9 UNIQUE, K24 active_pending_key), bu yüzden
 * kontrolle yazma arasındaki yarışlar da doğru hata koduna çevrilir.
 *
 * Bilerek @Transactional değil: catalog'a HTTP çağrısı DB transaction'ı açıkken yapılmasın.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KoltukTutmaServisi {

    public static final String COK_FAZLA_KOLTUK_KODU = "TOO_MANY_SEATS";
    public static final String TEKRARLI_KOLTUK_KODU = "DUPLICATE_SEATS";
    public static final String GECERSIZ_KOLTUK_KODU = "INVALID_SEATS";
    public static final String SATIS_KAPALI_KODU = "SALES_CLOSED";
    public static final String KOLTUK_DOLU_KODU = "SEATS_UNAVAILABLE";
    public static final String AKTIF_REZERVASYON_KODU = "ACTIVE_RESERVATION_EXISTS";

    private final KatalogIstemcisi katalogIstemcisi;
    private final KoltukTutmaIslemi tutmaIslemi;
    private final TerkKorumaDenetcisi terkKorumaDenetcisi;
    private final KoltukKilitDeposu kilitDeposu;
    private final RezervasyonRepository rezervasyonRepository;
    private final RezervasyonMapper rezervasyonMapper;
    private final DolulukOnbellegi dolulukOnbellegi;
    private final RezervasyonAyarlari ayarlar;
    private final Clock saat;

    public RezervasyonCevabi tut(Long kullaniciId, KoltukTutmaIstegi istek) {
        Set<Long> hucreler = hucreleriDogrula(istek.seatCellIds());
        Instant istekAni = saat.instant();
        aktifRezervasyonVarsaReddet(kullaniciId, istek.showtimeId(), istekAni);
        terkKorumaDenetcisi.denetle(kullaniciId, istekAni); // K29, catalog'a gitmeden (fail fast)
        KatalogSeansBilgisi seans = katalogIstemcisi.seansBilgisi(istek.showtimeId());
        List<RezervasyonKoltugu> koltuklar = koltuklariCoz(seans, hucreler);
        // Catalog çağrıları zaman aldı; süre hesabı yazma anından. Milisaniyeye yuvarlanır: DB DATETIME(3) saklar,
        // cevaptaki expiresAt ile sonradan okunan değer birebir aynı olsun
        Instant simdi = saat.instant().truncatedTo(ChronoUnit.MILLIS);
        Instant bitis = tutmaBitisi(seans, simdi);
        Rezervasyon rezervasyon = kaydet(kullaniciId, seans, koltuklar, bitis, simdi);
        // K47: kullanıcı kendi tutmasını haritada hemen görsün (2 sn'lik önbelleği beklemeden)
        dolulukOnbellegi.sil(seans.showtimeId());
        return rezervasyonMapper.cevap(rezervasyon, simdi);
    }

    /** Sıralı küme: tekrar yok ve kilitler her işlemde aynı sırayla alınır (deadlock önleme, §4.3). */
    private Set<Long> hucreleriDogrula(List<Long> hucreIdleri) {
        Set<Long> hucreler = new TreeSet<>(hucreIdleri);
        if (hucreler.size() != hucreIdleri.size()) {
            throw new GecersizIstekIstisnasi(TEKRARLI_KOLTUK_KODU, "Aynı koltuk birden fazla kez seçilmiş.");
        }
        if (hucreler.size() > ayarlar.enFazlaKoltuk()) {
            throw new GecersizIstekIstisnasi(COK_FAZLA_KOLTUK_KODU,
                    "Bir işlemde en fazla " + ayarlar.enFazlaKoltuk() + " koltuk seçilebilir.");
        }
        return hucreler;
    }

    /** Süresi dolmuş bekleyen işlem engel değildir: yazarken EXPIRED yapılır (KoltukTutmaIslemi). */
    private void aktifRezervasyonVarsaReddet(Long kullaniciId, Long seansId, Instant simdi) {
        rezervasyonRepository
                .findByKullaniciIdAndSeansIdAndDurum(kullaniciId, seansId, RezervasyonDurumu.PENDING_PAYMENT)
                .filter(rezervasyon -> rezervasyon.getBitis().isAfter(simdi))
                .ifPresent(rezervasyon -> {
                    throw aktifRezervasyonVar(rezervasyon.getId());
                });
    }

    private List<RezervasyonKoltugu> koltuklariCoz(KatalogSeansBilgisi seans, Set<Long> hucreler) {
        KoltukCozumlemeSonucu sonuc = katalogIstemcisi.koltuklariCoz(seans.showtimeId(),
                new KoltukCozumlemeIstegi(List.copyOf(hucreler)));
        Set<Long> gecersiz = new TreeSet<>(hucreler);
        sonuc.seats().forEach(koltuk -> gecersiz.remove(koltuk.cellId()));
        if (!gecersiz.isEmpty()) {
            throw new GecersizIstekIstisnasi(GECERSIZ_KOLTUK_KODU, "Seçilen yerlerden bazıları bu seansta koltuk değil.",
                    Map.of("invalidCellIds", List.copyOf(gecersiz)));
        }
        return sonuc.seats().stream()
                .sorted(Comparator.comparing(KoltukCozumlemeSonucu.Koltuk::cellId))
                .map(koltuk -> new RezervasyonKoltugu(koltuk.cellId(), koltuk.label(), seans.ticketPrice()))
                .toList();
    }

    /** §2.4-2: bitiş = min(şimdi + hold süresi, satış kapanışı); kapanış geçtiyse satış kapalı (K10). */
    private Instant tutmaBitisi(KatalogSeansBilgisi seans, Instant simdi) {
        Instant holdBitisi = simdi.plus(ayarlar.holdSuresi());
        Instant bitis = holdBitisi.isBefore(seans.salesCloseAt()) ? holdBitisi : seans.salesCloseAt();
        if (!bitis.isAfter(simdi)) {
            throw new IsKuraliIstisnasi(SATIS_KAPALI_KODU, "Bu seansın satışı kapandı.");
        }
        return bitis;
    }

    private Rezervasyon kaydet(Long kullaniciId, KatalogSeansBilgisi seans, List<RezervasyonKoltugu> koltuklar,
            Instant bitis, Instant simdi) {
        try {
            return tutmaIslemi.olustur(kullaniciId, seansKopyasi(seans), koltuklar, bitis, simdi);
        } catch (KoltukAlinamadiIstisnasi e) {
            throw koltukDolu(seans.showtimeId(), koltuklar, e.getHucreId(), simdi);
        } catch (PessimisticLockingFailureException e) {
            // Kilit beklerken MySQL işlemi kurban seçtiyse (deadlock/zaman aşımı) koltuk başkasına geçmiştir.
            // Sıralı kilitleme yüzünden beklenmez; görülürse incelenmeli
            log.warn("Koltuk tutmada kilit çatışması (seans {}): {}", seans.showtimeId(), e.getMessage());
            throw koltukDolu(seans.showtimeId(), koltuklar, null, simdi);
        } catch (DataIntegrityViolationException e) {
            // Aynı kullanıcının eşzamanlı ikinci isteği: active_pending_key UNIQUE (K24)
            throw rezervasyonRepository.findByKullaniciIdAndSeansIdAndDurum(
                            kullaniciId, seans.showtimeId(), RezervasyonDurumu.PENDING_PAYMENT)
                    .map(mevcut -> (RuntimeException) aktifRezervasyonVar(mevcut.getId()))
                    .orElse(e);
        }
    }

    /** 409 cevabında şu an dolu olan tüm seçili koltuklar listelenir (istemci haritayı bunlarla günceller). */
    private CakismaIstisnasi koltukDolu(Long seansId, List<RezervasyonKoltugu> koltuklar, Long alinamayanHucre,
            Instant simdi) {
        List<Long> hucreler = koltuklar.stream().map(RezervasyonKoltugu::getHucreId).toList();
        Set<Long> dolu = new TreeSet<>(kilitDeposu.doluHucreler(seansId, hucreler, simdi));
        if (alinamayanHucre != null) {
            dolu.add(alinamayanHucre);
        }
        // İstemci 409 sonrası haritayı hemen yeniler (§8.5); eski önbellekle "boş" görmesin
        dolulukOnbellegi.sil(seansId);
        return new CakismaIstisnasi(KOLTUK_DOLU_KODU, "Seçtiğiniz koltuklardan bazıları az önce alındı.",
                Map.of("unavailableCellIds", new ArrayList<>(dolu)));
    }

    private static CakismaIstisnasi aktifRezervasyonVar(Long rezervasyonId) {
        return new CakismaIstisnasi(AKTIF_REZERVASYON_KODU,
                "Bu seans için ödemesi bekleyen bir işleminiz var.", Map.of("reservationId", rezervasyonId));
    }

    private static Rezervasyon.SeansKopyasi seansKopyasi(KatalogSeansBilgisi seans) {
        return new Rezervasyon.SeansKopyasi(seans.showtimeId(), seans.hallId(), seans.movieTitle(),
                seans.cinemaName(), seans.hallName(), seans.startsAt());
    }
}
