package com.sinema.catalog.film.mapper;

import com.sinema.catalog.film.dto.FilmDetayi;
import com.sinema.catalog.film.dto.FilmIstegi;
import com.sinema.catalog.film.dto.FilmOzeti;
import com.sinema.catalog.film.entity.Film;
import com.sinema.catalog.film.entity.FilmBilgileri;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface FilmMapper {

    @Mapping(target = "baslik", source = "title")
    @Mapping(target = "ozet", source = "overview")
    @Mapping(target = "afisYolu", source = "posterPath")
    @Mapping(target = "sureDakika", source = "runtimeMinutes")
    @Mapping(target = "vizyonTarihi", source = "releaseDate")
    @Mapping(target = "yasSiniri", source = "certification")
    @Mapping(target = "tmdbPuani", source = "voteAverage")
    @Mapping(target = "yonetmen", source = "director")
    @Mapping(target = "turler", source = "genres")
    @Mapping(target = "oyuncular", source = "cast")
    @Mapping(target = "akilliIsaretler", source = "contentDescriptors")
    FilmBilgileri filmBilgileri(FilmIstegi istek);

    @Mapping(target = "title", source = "baslik")
    @Mapping(target = "posterPath", source = "afisYolu")
    @Mapping(target = "runtimeMinutes", source = "sureDakika")
    @Mapping(target = "releaseDate", source = "vizyonTarihi")
    @Mapping(target = "certification", source = "yasSiniri")
    @Mapping(target = "voteAverage", source = "tmdbPuani")
    @Mapping(target = "genres", source = "turler")
    @Mapping(target = "visible", source = "gorunur")
    FilmOzeti filmOzeti(Film film);

    @Mapping(target = "title", source = "baslik")
    @Mapping(target = "overview", source = "ozet")
    @Mapping(target = "posterPath", source = "afisYolu")
    @Mapping(target = "runtimeMinutes", source = "sureDakika")
    @Mapping(target = "releaseDate", source = "vizyonTarihi")
    @Mapping(target = "certification", source = "yasSiniri")
    @Mapping(target = "voteAverage", source = "tmdbPuani")
    @Mapping(target = "director", source = "yonetmen")
    @Mapping(target = "genres", source = "turler")
    @Mapping(target = "cast", source = "oyuncular")
    @Mapping(target = "contentDescriptors", source = "akilliIsaretler")
    @Mapping(target = "visible", source = "gorunur")
    @Mapping(target = "tmdbSyncedAt", source = "tmdbSenkronZamani")
    FilmDetayi filmDetayi(Film film);
}
