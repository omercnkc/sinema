package com.sinema.booking.rezervasyon.mapper;

import com.sinema.booking.rezervasyon.dto.RezervasyonCevabi;
import com.sinema.booking.rezervasyon.entity.Rezervasyon;
import com.sinema.booking.rezervasyon.entity.RezervasyonKoltugu;
import java.time.Instant;
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
    RezervasyonCevabi cevap(Rezervasyon rezervasyon, Instant simdi);

    @Mapping(target = "cellId", source = "hucreId")
    @Mapping(target = "label", source = "etiket")
    RezervasyonCevabi.Koltuk koltuk(RezervasyonKoltugu koltuk);
}
