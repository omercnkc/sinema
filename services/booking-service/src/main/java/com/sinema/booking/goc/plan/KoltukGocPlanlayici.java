package com.sinema.booking.goc.plan;

import com.sinema.booking.goc.entity.GocNedeni;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Bilet göçünün eşleştirme kuralları (K35, K51); saf ve durumsuz, DB/HTTP bilmez (birim testle sınanır).
 *
 * <ol>
 *   <li>Aynı konum: eski (satır, sütun) yeni düzende hâlâ koltuksa bilet oraya geçer; etiket aynıysa
 *       {@code SAME_POSITION}, değiştiyse {@code LABEL_CHANGED}. Bu adım bütün rezervasyonlar için yedeklerden önce
 *       yapılır: birinin aynı konumu başkasına yedek olarak verilmez.</li>
 *   <li>Yedek: yerinden çıkan koltuklar rezervasyon başına bir blok olarak, aynı sırada ve sütunları ardışık (araya
 *       koridor girmeyen) boş koltuklara yerleşir. Engelli bilet sayısı kadar engelli koltuk şarttır; fazla engelli
 *       koltuk kullanmak en ağır cezadır (o koltuklar ihtiyacı olanlara kalsın). Sonra eski konumlara toplam uzaklık,
 *       sonra perde ortasına uzaklık, en son sıra/sütun (deterministik).</li>
 *   <li>Önce çok koltuklu rezervasyonlar yerleşir (daha zor), eşitlikte eski rezervasyon önce. Blok bulunamayan
 *       rezervasyon çözülemez; diğerleri yine planlanır ki admin tüm sorunları bir kerede görsün.</li>
 * </ol>
 */
@Component
public class KoltukGocPlanlayici {

    private static final Comparator<Blok> EN_IYI_BLOK = Comparator
            .comparingInt(Blok::fazlaEngelli)
            .thenComparingDouble(Blok::toplamUzaklik)
            .thenComparingDouble(Blok::perdeFarki)
            .thenComparingInt(Blok::satir)
            .thenComparingInt(Blok::ilkSutun);

    public SeansGocPlani planla(long seansId, GocDuzeni eskiDuzen, GocDuzeni yeniDuzen, List<DoluKoltuk> dolular) {
        Map<Long, GocHucresi> eskiHucreler = eskiDuzen.hucreIndeksi();
        List<GocHucresi> yeniKoltuklar = yeniDuzen.koltuklar();
        Set<Long> doluYeniHucreler = new HashSet<>();
        List<KoltukTasima> tasimalar = new ArrayList<>();
        Map<Long, List<GocHucresi>> yerindenCikanlar = new TreeMap<>();

        for (DoluKoltuk dolu : siraliDolular(dolular)) {
            GocHucresi eski = eskiHucre(eskiHucreler, dolu, seansId);
            Optional<GocHucresi> ayniYer = yeniKoltuklar.stream().filter(eski::ayniKonumda).findFirst();
            if (ayniYer.isPresent()) {
                GocHucresi yeni = ayniYer.get();
                doluYeniHucreler.add(yeni.hucreId());
                GocNedeni neden = yeni.etiket().equals(eski.etiket()) ? GocNedeni.SAME_POSITION : GocNedeni.LABEL_CHANGED;
                tasimalar.add(tasima(dolu.rezervasyonId(), eski, yeni, neden));
            } else {
                yerindenCikanlar.computeIfAbsent(dolu.rezervasyonId(), id -> new ArrayList<>()).add(eski);
            }
        }

        double perdeMerkezi = yeniDuzen.perdeMerkezi();
        Map<Integer, List<GocHucresi>> sirayaGore = sirayaGoreKoltuklar(yeniKoltuklar);
        List<CozulemeyenRezervasyon> cozulemeyenler = new ArrayList<>();
        yerlesimSirasi(yerindenCikanlar).forEach((rezervasyonId, eskiler) -> {
            Optional<Blok> blok = enIyiBlok(eskiler, sirayaGore, doluYeniHucreler, perdeMerkezi);
            if (blok.isEmpty()) {
                cozulemeyenler.add(new CozulemeyenRezervasyon(rezervasyonId,
                        eskiler.stream().map(GocHucresi::etiket).sorted().toList()));
                return;
            }
            blok.get().eslesmeler().forEach((eski, yeni) -> {
                doluYeniHucreler.add(yeni.hucreId());
                tasimalar.add(tasima(rezervasyonId, eski, yeni, GocNedeni.REPLACEMENT));
            });
        });
        return new SeansGocPlani(seansId, tasimalar, cozulemeyenler);
    }

    private static List<DoluKoltuk> siraliDolular(List<DoluKoltuk> dolular) {
        return dolular.stream()
                .sorted(Comparator.comparingLong(DoluKoltuk::rezervasyonId).thenComparingLong(DoluKoltuk::hucreId))
                .toList();
    }

    private static GocHucresi eskiHucre(Map<Long, GocHucresi> eskiHucreler, DoluKoltuk dolu, long seansId) {
        GocHucresi eski = eskiHucreler.get(dolu.hucreId());
        if (eski == null || !eski.koltuk()) {
            // Doluluk kaydı seansın mühürlü düzeninin dışındaysa veri tutarsızdır; göç tahminle yapılmaz
            throw new IllegalStateException("Seans " + seansId + " için düzende olmayan koltuk: " + dolu.hucreId());
        }
        return eski;
    }

    /** Çok koltuklu önce (daha zor), eşitlikte eski rezervasyon (küçük kimlik) önce. */
    private static Map<Long, List<GocHucresi>> yerlesimSirasi(Map<Long, List<GocHucresi>> yerindenCikanlar) {
        return yerindenCikanlar.entrySet().stream()
                .sorted(Comparator.<Map.Entry<Long, List<GocHucresi>>>comparingInt(giris -> -giris.getValue().size())
                        .thenComparingLong(Map.Entry::getKey))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    private static Map<Integer, List<GocHucresi>> sirayaGoreKoltuklar(List<GocHucresi> koltuklar) {
        return koltuklar.stream().collect(Collectors.groupingBy(GocHucresi::satir, TreeMap::new,
                Collectors.collectingAndThen(Collectors.toList(), satir -> satir.stream()
                        .sorted(Comparator.comparingInt(GocHucresi::sutun)).toList())));
    }

    private static Optional<Blok> enIyiBlok(List<GocHucresi> eskiler, Map<Integer, List<GocHucresi>> sirayaGore,
            Set<Long> dolular, double perdeMerkezi) {
        List<GocHucresi> siraliEskiler = eskiler.stream().sorted(Comparator.comparingInt(GocHucresi::sutun)).toList();
        int gerekenEngelli = (int) eskiler.stream().filter(GocHucresi::engelli).count();
        List<Blok> adaylar = new ArrayList<>();
        for (List<GocHucresi> satir : sirayaGore.values()) {
            for (int bas = 0; bas + eskiler.size() <= satir.size(); bas++) {
                List<GocHucresi> pencere = satir.subList(bas, bas + eskiler.size());
                if (bitisikVeBos(pencere, dolular)) {
                    eslestir(siraliEskiler, pencere, gerekenEngelli, perdeMerkezi).ifPresent(adaylar::add);
                }
            }
        }
        return adaylar.stream().min(EN_IYI_BLOK);
    }

    private static boolean bitisikVeBos(List<GocHucresi> pencere, Set<Long> dolular) {
        for (int i = 0; i < pencere.size(); i++) {
            if (dolular.contains(pencere.get(i).hucreId())
                    || (i > 0 && pencere.get(i).sutun() != pencere.get(i - 1).sutun() + 1)) {
                return false;
            }
        }
        return true;
    }

    /** Engelli biletler bloğun engelli koltuklarına, kalanlar diğerlerine soldan sağa; yeterli engelli yoksa blok olmaz. */
    private static Optional<Blok> eslestir(List<GocHucresi> eskiler, List<GocHucresi> pencere, int gerekenEngelli,
            double perdeMerkezi) {
        List<GocHucresi> engelliYerler = pencere.stream().filter(GocHucresi::engelli).toList();
        if (engelliYerler.size() < gerekenEngelli) {
            return Optional.empty();
        }
        List<GocHucresi> bosEngelli = new ArrayList<>(engelliYerler);
        List<GocHucresi> digerYerler = new ArrayList<>(pencere.stream().filter(yer -> !yer.engelli()).toList());
        Map<GocHucresi, GocHucresi> eslesmeler = new LinkedHashMap<>();
        for (GocHucresi eski : eskiler.stream().filter(GocHucresi::engelli).toList()) {
            eslesmeler.put(eski, bosEngelli.removeFirst());
        }
        // Engelli olmayan biletler önce engelli olmayan yerlere; yetmezse kalan engelli yerlere (fazlalık cezalı)
        digerYerler.addAll(bosEngelli);
        List<GocHucresi> standartlar = eskiler.stream().filter(eski -> !eski.engelli()).toList();
        for (int i = 0; i < standartlar.size(); i++) {
            eslesmeler.put(standartlar.get(i), digerYerler.get(i));
        }
        double toplamUzaklik = eslesmeler.entrySet().stream().mapToDouble(e -> e.getKey().uzaklik(e.getValue())).sum();
        double blokMerkezi = pencere.stream().mapToInt(GocHucresi::sutun).average().orElse(0);
        return Optional.of(new Blok(eslesmeler, engelliYerler.size() - gerekenEngelli, toplamUzaklik,
                Math.abs(blokMerkezi - perdeMerkezi), pencere.getFirst().satir(), pencere.getFirst().sutun()));
    }

    private static KoltukTasima tasima(long rezervasyonId, GocHucresi eski, GocHucresi yeni, GocNedeni neden) {
        return new KoltukTasima(rezervasyonId, eski.hucreId(), eski.etiket(), yeni.hucreId(), yeni.etiket(), neden);
    }

    /** Aday blok: eski koltuk → yeni koltuk eşleşmesi ve karşılaştırma ölçütleri. */
    private record Blok(Map<GocHucresi, GocHucresi> eslesmeler, int fazlaEngelli, double toplamUzaklik,
            double perdeFarki, int satir, int ilkSutun) {
    }
}
