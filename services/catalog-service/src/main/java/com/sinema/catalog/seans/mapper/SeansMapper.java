package com.sinema.catalog.seans.mapper;

import com.sinema.catalog.duzen.entity.DuzenHucresi;
import com.sinema.catalog.seans.dto.IcSeansBilgisi;
import com.sinema.catalog.seans.dto.KoltukCozumlemeCevabi;
import com.sinema.catalog.seans.dto.SeansDetayi;
import com.sinema.catalog.seans.dto.SeansOzeti;
import com.sinema.catalog.seans.entity.Seans;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** "Satış açık mı" zamana bağlıdır; şimdiki an servisin saatinden parametre olarak gelir. */
@Mapper
public interface SeansMapper {

    @Mapping(target = "startsAt", source = "seans.baslangic")
    @Mapping(target = "endsAt", source = "seans.bitis")
    @Mapping(target = "cinemaId", source = "seans.salon.sube.id")
    @Mapping(target = "cinemaName", source = "seans.salon.sube.ad")
    @Mapping(target = "hallId", source = "seans.salon.id")
    @Mapping(target = "hallName", source = "seans.salon.ad")
    @Mapping(target = "ticketPrice", source = "seans.biletFiyati")
    @Mapping(target = "salesCloseAt", expression = "java(seans.satisKapanisi())")
    @Mapping(target = "salesOpen", expression = "java(seans.satisAcikMi(simdi))")
    SeansOzeti seansOzeti(Seans seans, Instant simdi);

    @Mapping(target = "movieId", source = "seans.film.id")
    @Mapping(target = "movieTitle", source = "seans.film.baslik")
    @Mapping(target = "posterPath", source = "seans.film.afisYolu")
    @Mapping(target = "certification", source = "seans.film.yasSiniri")
    @Mapping(target = "cinemaId", source = "seans.salon.sube.id")
    @Mapping(target = "cinemaName", source = "seans.salon.sube.ad")
    @Mapping(target = "hallId", source = "seans.salon.id")
    @Mapping(target = "hallName", source = "seans.salon.ad")
    @Mapping(target = "layoutId", source = "seans.duzenId")
    @Mapping(target = "startsAt", source = "seans.baslangic")
    @Mapping(target = "endsAt", source = "seans.bitis")
    @Mapping(target = "ticketPrice", source = "seans.biletFiyati")
    @Mapping(target = "salesCloseOffsetMinutes", source = "seans.satisKapanisOfsetiDakika")
    @Mapping(target = "salesCloseAt", expression = "java(seans.satisKapanisi())")
    @Mapping(target = "salesOpen", expression = "java(seans.satisAcikMi(simdi))")
    @Mapping(target = "serverTime", source = "simdi")
    SeansDetayi seansDetayi(Seans seans, Instant simdi);

    @Mapping(target = "showtimeId", source = "seans.id")
    @Mapping(target = "movieId", source = "seans.film.id")
    @Mapping(target = "movieTitle", source = "seans.film.baslik")
    @Mapping(target = "cinemaId", source = "seans.salon.sube.id")
    @Mapping(target = "cinemaName", source = "seans.salon.sube.ad")
    @Mapping(target = "hallId", source = "seans.salon.id")
    @Mapping(target = "hallName", source = "seans.salon.ad")
    @Mapping(target = "layoutId", source = "seans.duzenId")
    @Mapping(target = "startsAt", source = "seans.baslangic")
    @Mapping(target = "endsAt", source = "seans.bitis")
    @Mapping(target = "salesCloseAt", expression = "java(seans.satisKapanisi())")
    @Mapping(target = "ticketPrice", source = "seans.biletFiyati")
    @Mapping(target = "serverTime", source = "simdi")
    IcSeansBilgisi icSeansBilgisi(Seans seans, Instant simdi);

    /** Etiket biçimi biletteki gibi: sıra harfi + "-" + numara (ör. "F-7", planlama §2.7). */
    @Mapping(target = "cellId", source = "id")
    @Mapping(target = "seatRowLabel", source = "siraEtiketi")
    @Mapping(target = "seatNumber", source = "koltukNumarasi")
    @Mapping(target = "label", expression = "java(hucre.getSiraEtiketi() + \"-\" + hucre.getKoltukNumarasi())")
    @Mapping(target = "seatType", source = "koltukTipi")
    KoltukCozumlemeCevabi.Koltuk koltuk(DuzenHucresi hucre);
}
