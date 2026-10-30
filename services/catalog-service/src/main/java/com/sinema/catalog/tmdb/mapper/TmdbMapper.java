package com.sinema.catalog.tmdb.mapper;

import com.sinema.catalog.film.entity.FilmBilgileri;
import com.sinema.catalog.tmdb.dto.TmdbAramaSonucu;
import com.sinema.catalog.tmdb.dto.TmdbFilmOnizlemesi;
import com.sinema.catalog.tmdb.istemci.TmdbAramaCevabi;
import com.sinema.catalog.tmdb.service.TmdbDonusturucu;
import java.time.LocalDate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface TmdbMapper {

    /** Yenileme için; süre TMDB'de yoksa 0 olur ve birleştirmede mevcut süre korunur. */
    @Mapping(target = "baslik", source = "title")
    @Mapping(target = "ozet", source = "overview")
    @Mapping(target = "afisYolu", source = "posterPath")
    @Mapping(target = "sureDakika", source = "runtimeMinutes", defaultValue = "0")
    @Mapping(target = "vizyonTarihi", source = "releaseDate")
    @Mapping(target = "yasSiniri", source = "certification")
    @Mapping(target = "tmdbPuani", source = "voteAverage")
    @Mapping(target = "yonetmen", source = "director")
    @Mapping(target = "turler", source = "genres")
    @Mapping(target = "oyuncular", source = "cast")
    @Mapping(target = "akilliIsaretler", source = "contentDescriptors")
    FilmBilgileri filmBilgileri(TmdbFilmOnizlemesi onizleme);

    /** movieId çağıranda (DB'ye bakarak) doldurulur. */
    @Mapping(target = "tmdbId", source = "id")
    @Mapping(target = "movieId", ignore = true)
    TmdbAramaSonucu.Aday aday(TmdbAramaCevabi.Sonuc sonuc);

    default LocalDate tarih(String deger) {
        return TmdbDonusturucu.tarih(deger);
    }
}
