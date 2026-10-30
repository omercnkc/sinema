package com.sinema.catalog.sube.mapper;

import com.sinema.catalog.sube.dto.SalonCevabi;
import com.sinema.catalog.sube.dto.SubeCevabi;
import com.sinema.catalog.sube.dto.YonetimSubeCevabi;
import com.sinema.catalog.sube.entity.Salon;
import com.sinema.catalog.sube.entity.Sube;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Entity → cevap DTO dönüşümleri. Kodu MapStruct derleme anında üretir (Mapper pattern, rules §4). */
@Mapper
public interface SubeMapper {

    @Mapping(target = "name", source = "ad")
    @Mapping(target = "city", source = "sehir")
    @Mapping(target = "address", source = "adres")
    @Mapping(target = "halls", source = "salonlar")
    SubeCevabi subeCevabi(Sube sube);

    List<SubeCevabi> subeCevaplari(List<Sube> subeler);

    @Mapping(target = "name", source = "ad")
    SubeCevabi.SalonOzeti salonOzeti(Salon salon);

    @Mapping(target = "name", source = "ad")
    @Mapping(target = "city", source = "sehir")
    @Mapping(target = "address", source = "adres")
    @Mapping(target = "halls", source = "salonlar")
    YonetimSubeCevabi yonetimSubeCevabi(Sube sube);

    List<YonetimSubeCevabi> yonetimSubeCevaplari(List<Sube> subeler);

    @Mapping(target = "cinemaId", source = "sube.id")
    @Mapping(target = "name", source = "ad")
    @Mapping(target = "defaultTicketPrice", source = "varsayilanBiletFiyati")
    @Mapping(target = "activeLayoutId", source = "aktifDuzenId")
    SalonCevabi salonCevabi(Salon salon);
}
