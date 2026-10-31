package com.sinema.auth.kullanici.dto;

/**
 * payment-service'in iyzico alıcı bilgisi için okuduğu kişi bilgileri (iç uç, K48). Kişi alanları admin ve eski
 * hesaplarda boş olabilir; payment eksik alanı sabit değerle doldurur. Kullanıcıya dönen ProfilCevabi'ndan ayrı:
 * iki sözleşme bağımsız değişebilsin.
 */
public record AliciBilgisi(Long userId, String email, String firstName, String lastName, String phone) {

    @Override
    public String toString() {
        return "AliciBilgisi[userId=" + userId + ", email=***, firstName=***, lastName=***, phone=***]";
    }
}
