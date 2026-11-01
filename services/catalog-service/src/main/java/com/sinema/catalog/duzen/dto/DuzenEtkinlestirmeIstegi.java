package com.sinema.catalog.duzen.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Göçün son adımı (booking → catalog): booking'in biletlerini taşıdığı seanslar. Boş olabilir (biletsiz salonda
 * göç yalnızca yayımlar). Catalog, listedekilerin hâlâ başlamamış olduğunu doğrular.
 */
public record DuzenEtkinlestirmeIstegi(@NotNull List<@NotNull Long> showtimeIds) {
}
