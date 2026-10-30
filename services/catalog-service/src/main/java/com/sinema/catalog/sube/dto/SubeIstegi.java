package com.sinema.catalog.sube.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Şube oluşturma ve güncelleme (aynı alanlar). */
public record SubeIstegi(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 255) String address) {
}
