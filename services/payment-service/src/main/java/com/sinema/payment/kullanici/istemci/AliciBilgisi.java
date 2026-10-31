package com.sinema.payment.kullanici.istemci;

/**
 * auth'un billing-info cevabı (auth AliciBilgisi). Kişi alanları admin ve eski hesaplarda boş olabilir; eksik alan
 * ödeme başlatılırken sabit değerle doldurulur (K48). Kişisel veri: loglanmaz.
 */
public record AliciBilgisi(Long userId, String email, String firstName, String lastName, String phone) {

    @Override
    public String toString() {
        return "AliciBilgisi[userId=" + userId + ", email=***, firstName=***, lastName=***, phone=***]";
    }
}
