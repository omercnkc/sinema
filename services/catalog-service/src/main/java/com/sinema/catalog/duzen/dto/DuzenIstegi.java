package com.sinema.catalog.duzen.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Taslak düzen oluşturma ve güncelleme (tüm hücreler her seferinde baştan gönderilir).
 * Buradaki kurallar biçimseldir; ızgara sınırı, çakışan konum, perde zorunluluğu gibi
 * düzenin bütününe dair kurallar DuzenDogrulayici'dadır.
 */
public record DuzenIstegi(
        @NotNull @Min(1) @Max(EN_COK_SATIR) Integer gridRows,
        @NotNull @Min(1) @Max(EN_COK_SUTUN) Integer gridCols,
        @NotNull @Size(max = EN_COK_HUCRE) List<@NotNull @Valid HucreIstegi> cells) {

    public static final int EN_COK_SATIR = 50;
    public static final int EN_COK_SUTUN = 50;
    public static final int EN_COK_HUCRE = EN_COK_SATIR * EN_COK_SUTUN;
}
