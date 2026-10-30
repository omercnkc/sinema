package com.sinema.catalog.duzen.mapper;

import com.sinema.catalog.duzen.dto.DuzenCevabi;
import com.sinema.catalog.duzen.dto.DuzenOzeti;
import com.sinema.catalog.duzen.dto.HucreCevabi;
import com.sinema.catalog.duzen.entity.DuzenHucresi;
import com.sinema.catalog.duzen.entity.SalonDuzeni;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface DuzenMapper {

    @Mapping(target = "cellId", source = "id")
    @Mapping(target = "row", source = "satir")
    @Mapping(target = "col", source = "sutun")
    @Mapping(target = "type", source = "tip")
    @Mapping(target = "seatRowLabel", source = "siraEtiketi")
    @Mapping(target = "seatNumber", source = "koltukNumarasi")
    @Mapping(target = "seatType", source = "koltukTipi")
    HucreCevabi hucreCevabi(DuzenHucresi hucre);

    /** "aktif" bilgisi düzende değil salonda durur; çağıran hesaplayıp verir. */
    @Mapping(target = "id", source = "duzen.id")
    @Mapping(target = "hallId", source = "duzen.salonId")
    @Mapping(target = "version", source = "duzen.surum")
    @Mapping(target = "status", source = "duzen.durum")
    @Mapping(target = "active", source = "aktif")
    @Mapping(target = "gridRows", source = "duzen.satirSayisi")
    @Mapping(target = "gridCols", source = "duzen.sutunSayisi")
    @Mapping(target = "createdAt", source = "duzen.olusturulmaZamani")
    @Mapping(target = "publishedAt", source = "duzen.yayimlanmaZamani")
    @Mapping(target = "cells", source = "hucreler")
    DuzenCevabi duzenCevabi(SalonDuzeni duzen, List<DuzenHucresi> hucreler, boolean aktif);

    @Mapping(target = "id", source = "duzen.id")
    @Mapping(target = "version", source = "duzen.surum")
    @Mapping(target = "status", source = "duzen.durum")
    @Mapping(target = "active", source = "aktif")
    @Mapping(target = "gridRows", source = "duzen.satirSayisi")
    @Mapping(target = "gridCols", source = "duzen.sutunSayisi")
    @Mapping(target = "seatCount", source = "koltukSayisi")
    @Mapping(target = "createdAt", source = "duzen.olusturulmaZamani")
    @Mapping(target = "publishedAt", source = "duzen.yayimlanmaZamani")
    DuzenOzeti duzenOzeti(SalonDuzeni duzen, long koltukSayisi, boolean aktif);
}
