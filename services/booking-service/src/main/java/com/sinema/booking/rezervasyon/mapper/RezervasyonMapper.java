package com.sinema.booking.rezervasyon.mapper;

import com.sinema.booking.rezervasyon.dto.RezervasyonCevabi;
import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonKoltugu;
import com.sinema.booking.goc.entity.KoltukGocKaydi;
import java.time.Instant;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Sunucu saati cevaba eklenir (geri sayım için); servisin saatinden parametre olarak gelir. */
@Mapper
public interface RezervasyonMapper {

    @Mapping(target = "reservationId", source = "rezervasyon.id")
    @Mapping(target = "showtimeId", source = "rezervasyon.seansId")
    @Mapping(target = "status", source = "rezervasyon.durum")
    @Mapping(target = "movieTitle", source = "rezervasyon.filmAdi")
    @Mapping(target = "cinemaName", source = "rezervasyon.subeAdi")
    @Mapping(target = "hallName", source = "rezervasyon.salonAdi")
    @Mapping(target = "startsAt", source = "rezervasyon.seansBaslangici")
    @Mapping(target = "expiresAt", source = "rezervasyon.bitis")
    @Mapping(target = "serverTime", source = "simdi")
    @Mapping(target = "seats", source = "rezervasyon.koltuklar")
    @Mapping(target = "totalAmount", source = "rezervasyon.toplamTutar")
    @Mapping(target = "ticketCode", source = "rezervasyon.biletKodu")
    @Mapping(target = "seatChanges", source = "koltukDegisiklikleri")
    RezervasyonCevabi cevap(Rezervasyon rezervasyon, Instant simdi,
            List<RezervasyonCevabi.KoltukDegisikligi> koltukDegisiklikleri);

    /** Tutma ve liste cevabı: koltuk değişiklikleri tekil sorguda doldurulur. */
    default RezervasyonCevabi cevap(Rezervasyon rezervasyon, Instant simdi) {
        return cevap(rezervasyon, simdi, List.of());
    }

    @Mapping(target = "oldLabel", source = "eskiEtiket")
    @Mapping(target = "newLabel", source = "yeniEtiket")
    @Mapping(target = "reason", source = "neden")
    @Mapping(target = "changedAt", source = "olusturulmaZamani")
    RezervasyonCevabi.KoltukDegisikligi koltukDegisikligi(KoltukGocKaydi kayit);

    @Mapping(target = "cellId", source = "hucreId")
    @Mapping(target = "label", source = "etiket")
    RezervasyonCevabi.Koltuk koltuk(RezervasyonKoltugu koltuk);
}
