package com.sinema.catalog.ornekveri;

import java.util.List;

/**
 * Örnek salon düzeninin tarifi; OrnekDuzenUretici bunu hücrelere çevirir.
 *
 * @param siraSayisi       koltuk sırası sayısı (A, B, C...)
 * @param siradakiKoltuk   bir sıradaki en çok koltuk
 * @param koridorlar       hangi koltuktan sonra koridor açılır (ör. [7]: 7. koltuktan sonra bir sütun boşluk)
 * @param girisSolda       giriş solda, çıkış sağda (false ise tersi)
 * @param eksikKoseSirasi  öndeki bu kadar sırada iki uç koltuk yok (L/çanak biçimli salonlar için)
 */
public record DuzenSablonu(int siraSayisi, int siradakiKoltuk, List<Integer> koridorlar, boolean girisSolda,
        int eksikKoseSirasi) {
}
