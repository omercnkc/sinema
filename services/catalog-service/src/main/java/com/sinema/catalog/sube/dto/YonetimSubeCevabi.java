package com.sinema.catalog.sube.dto;

import java.util.List;

/** Admin paneli için şube: salonlar fiyat ve aktif düzen bilgisiyle döner. */
public record YonetimSubeCevabi(Long id, String name, String city, String address, List<SalonCevabi> halls) {
}
