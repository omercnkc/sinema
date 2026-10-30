package com.sinema.catalog.sube.controller;

import com.sinema.catalog.sube.dto.SubeCevabi;
import com.sinema.catalog.sube.service.SubeServisi;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Herkese açık katalog (misafir dahil, K3); application.yml'de GET /api/v1/catalog/** açık yol. */
@RestController
@RequestMapping("/api/v1/catalog/cinemas")
@RequiredArgsConstructor
public class SubeKontrolcusu {

    private final SubeServisi subeServisi;

    @GetMapping
    public List<SubeCevabi> listele() {
        return subeServisi.listele();
    }
}
