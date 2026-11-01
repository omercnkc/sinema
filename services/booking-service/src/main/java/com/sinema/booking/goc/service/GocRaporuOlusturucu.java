package com.sinema.booking.goc.service;

import com.sinema.booking.goc.dto.GocRaporu;
import com.sinema.booking.goc.dto.GocRaporu.Cozulemeyen;
import com.sinema.booking.goc.dto.GocRaporu.Degisiklik;
import com.sinema.booking.goc.dto.GocRaporu.Ozet;
import com.sinema.booking.goc.dto.GocRaporu.SeansRaporu;
import com.sinema.booking.goc.entity.GocNedeni;
import com.sinema.booking.goc.plan.KoltukTasima;
import com.sinema.booking.goc.plan.SeansGocPlani;
import com.sinema.booking.katalog.istemci.KatalogGocKaynagi;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Göç hesabını önizleme raporuna çevirir (Mapper; MapStruct'a değmeyecek kadar küçük ama sayımlar içeriyor). */
@Component
public class GocRaporuOlusturucu {

    public GocRaporu rapor(Long salonId, Long hedefDuzenId, GocHesabi hesap) {
        Map<Long, Instant> baslangiclar = hesap.kaynak().showtimes().stream()
                .collect(Collectors.toMap(KatalogGocKaynagi.Seans::id, KatalogGocKaynagi.Seans::startsAt));
        List<SeansRaporu> seanslar = hesap.planlar().stream()
                .map(plan -> seansRaporu(plan, baslangiclar.get(plan.seansId())))
                .sorted(Comparator.comparing(SeansRaporu::startsAt))
                .toList();
        List<KoltukTasima> tasimalar = hesap.planlar().stream().flatMap(plan -> plan.tasimalar().stream()).toList();
        Ozet ozet = new Ozet(hesap.kaynak().showtimes().size(), tasimalar.size(),
                say(tasimalar, GocNedeni.SAME_POSITION), say(tasimalar, GocNedeni.LABEL_CHANGED),
                say(tasimalar, GocNedeni.REPLACEMENT),
                hesap.planlar().stream().mapToInt(plan -> plan.cozulemeyenler().size()).sum());
        return new GocRaporu(salonId, hedefDuzenId, hesap.uygulanabilir(), ozet, seanslar);
    }

    private static SeansRaporu seansRaporu(SeansGocPlani plan, Instant baslangic) {
        List<Degisiklik> degisiklikler = plan.tasimalar().stream()
                .filter(tasima -> tasima.neden().bildirimGerekir())
                .map(tasima -> new Degisiklik(tasima.rezervasyonId(), tasima.eskiEtiket(), tasima.yeniEtiket(),
                        tasima.neden()))
                .toList();
        List<Cozulemeyen> cozulemeyenler = plan.cozulemeyenler().stream()
                .map(rezervasyon -> new Cozulemeyen(rezervasyon.rezervasyonId(), rezervasyon.eskiEtiketler()))
                .toList();
        return new SeansRaporu(plan.seansId(), baslangic, say(plan.tasimalar(), GocNedeni.SAME_POSITION),
                degisiklikler, cozulemeyenler);
    }

    private static int say(List<KoltukTasima> tasimalar, GocNedeni neden) {
        return (int) tasimalar.stream().filter(tasima -> tasima.neden() == neden).count();
    }
}
