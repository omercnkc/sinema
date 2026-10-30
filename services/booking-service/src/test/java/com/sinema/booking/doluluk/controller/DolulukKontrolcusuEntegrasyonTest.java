package com.sinema.booking.doluluk.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class DolulukKontrolcusuEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    @Test
    void shouldListSoldAndActiveHeldSeatsForGuest_withoutHolderAndWithoutCaching() throws Exception {
        long seansId = yeniSeansId();
        tut(51, seansId, "[2]");
        satilmisKoltukEkle(seansId, 1);
        suresiDolmusTutmaEkle(seansId, 3, 52);

        doluluk(seansId)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.serverTime").exists())
                .andExpect(jsonPath("$.occupied[*].cellId", contains(1, 2)))
                .andExpect(jsonPath("$.occupied[*].status", contains("SOLD", "HELD")))
                .andExpect(jsonPath("$.occupied[0].userId").doesNotExist());
    }

    @Test
    void shouldServeCachedResult_whenSeatChangesOutsideHoldFlowWithinCacheLifetime() throws Exception {
        long seansId = yeniSeansId();
        doluluk(seansId).andExpect(jsonPath("$.occupied", empty()));

        satilmisKoltukEkle(seansId, 1); // hold akışı dışında yazım: önbellek silinmez

        doluluk(seansId).andExpect(jsonPath("$.occupied", empty()));
    }

    @Test
    void shouldShowOwnHoldImmediately_whenHoldInvalidatesCache() throws Exception {
        long seansId = yeniSeansId();
        doluluk(seansId).andExpect(jsonPath("$.occupied", empty()));

        tut(53, seansId, "[4, 5]");

        doluluk(seansId).andExpect(jsonPath("$.occupied[*].cellId", contains(4, 5)));
    }

    @Test
    void shouldRefreshMap_whenHoldFailsBecauseSeatWasTaken() throws Exception {
        long seansId = yeniSeansId();
        doluluk(seansId).andExpect(jsonPath("$.occupied", empty()));
        satilmisKoltukEkle(seansId, 6);

        tut(54, seansId, "[6]").andExpect(status().isConflict());

        doluluk(seansId).andExpect(jsonPath("$.occupied", hasSize(1)));
    }

    private ResultActions doluluk(long seansId) throws Exception {
        return mockMvc.perform(get("/api/v1/bookings/showtimes/{id}/seat-status", seansId));
    }

    private void satilmisKoltukEkle(long seansId, long hucreId) {
        Timestamp simdi = Timestamp.from(Instant.now());
        long rezervasyonId = bekleyenRezervasyonEkle(seansId, 900 + hucreId, simdi);
        jdbcTemplate.update("""
                INSERT INTO seat_bookings (showtime_id, layout_cell_id, status, hold_expires_at, user_id,
                                           reservation_id, version, created_at, updated_at)
                VALUES (?, ?, 'SOLD', NULL, ?, ?, 0, ?, ?)""",
                seansId, hucreId, 900 + hucreId, rezervasyonId, simdi, simdi);
    }
}
