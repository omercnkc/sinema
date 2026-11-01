package com.sinema.booking.bildirim.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sinema.booking.rezervasyon.RezervasyonEntegrasyonTesti;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Uygulama içi bildirimler (K35): yalnızca kendi bildirimleri, okunmamışlar önce. */
class BildirimKontrolcusuEntegrasyonTest extends RezervasyonEntegrasyonTesti {

    private static final String YOL = "/api/v1/bookings/notifications";

    private long bildirimEkle(long kullaniciId, String mesaj, Instant olusturulma, boolean okundu) {
        jdbcTemplate.update("""
                INSERT INTO notifications (user_id, type, reservation_id, message, read_at, created_at)
                VALUES (?, 'SEAT_CHANGED', NULL, ?, ?, ?)""",
                kullaniciId, mesaj, okundu ? Timestamp.from(olusturulma) : null, Timestamp.from(olusturulma));
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM notifications WHERE user_id = ?", Long.class, kullaniciId);
    }

    private ResultActions istek(MockHttpServletRequestBuilder istek, long kullaniciId) throws Exception {
        return mockMvc.perform(istek.header("Authorization", yetki(kullaniciId)));
    }

    @Test
    void shouldListOwnNotificationsUnreadFirstThenNewest_andCountUnread() throws Exception {
        long kullanici = yeniKullaniciId();
        Instant simdi = Instant.now();
        bildirimEkle(kullanici, "eski okunmuş", simdi.minus(Duration.ofHours(3)), true);
        bildirimEkle(kullanici, "eski okunmamış", simdi.minus(Duration.ofHours(2)), false);
        bildirimEkle(kullanici, "yeni okunmuş", simdi.minus(Duration.ofMinutes(5)), true);
        bildirimEkle(kullanici, "yeni okunmamış", simdi.minus(Duration.ofMinutes(1)), false);
        bildirimEkle(yeniKullaniciId(), "başkasının", simdi, false);

        istek(get(YOL), kullanici)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].message").value("yeni okunmamış"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.content[0].type").value("SEAT_CHANGED"))
                .andExpect(jsonPath("$.content[1].message").value("eski okunmamış"))
                .andExpect(jsonPath("$.content[2].message").value("yeni okunmuş"))
                .andExpect(jsonPath("$.content[3].message").value("eski okunmuş"));
        istek(get(YOL + "/unread-count"), kullanici).andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void shouldMarkOwnNotificationReadIdempotently_andHideOthers() throws Exception {
        long kullanici = yeniKullaniciId();
        long bildirim = bildirimEkle(kullanici, "koltuğunuz değişti", Instant.now(), false);

        istek(post(YOL + "/{id}/read", bildirim), kullanici).andExpect(status().isNoContent());
        istek(post(YOL + "/{id}/read", bildirim), kullanici).andExpect(status().isNoContent());
        istek(get(YOL + "/unread-count"), kullanici).andExpect(jsonPath("$.count").value(0));

        istek(post(YOL + "/{id}/read", bildirim), yeniKullaniciId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        mockMvc.perform(get(YOL + "/unread-count")).andExpect(status().isUnauthorized());
    }
}
