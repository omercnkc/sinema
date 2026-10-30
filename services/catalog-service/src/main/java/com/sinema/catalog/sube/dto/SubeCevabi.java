package com.sinema.catalog.sube.dto;

import java.util.List;

/** Herkese açık şube listesi: salon fiyatı bilerek yok, fiyat seansta gösterilir (planlama §7.4). */
public record SubeCevabi(Long id, String name, String city, String address, List<SalonOzeti> halls) {

    public record SalonOzeti(Long id, String name) {
    }
}
