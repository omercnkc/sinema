package com.sinema.catalog.ornekveri;

import com.sinema.catalog.duzen.dto.DuzenIstegi;
import com.sinema.catalog.duzen.dto.HucreIstegi;
import com.sinema.catalog.duzen.entity.HucreTipi;
import com.sinema.catalog.duzen.entity.KoltukTipi;
import java.util.ArrayList;
import java.util.List;

/**
 * Şablondan düzen isteği üretir. Izgara yerleşimi:
 * <pre>
 *   satır 0      : perde (kenar sütunları hariç)
 *   satır 1      : boşluk (perde ile ilk sıra arası)
 *   satır 2..    : koltuk sıraları; sütun 0 ve son sütun yan koridor
 *   son satır    : arka koridor; bir uçta giriş, diğer uçta çıkış
 * </pre>
 * Arka sıranın ilk iki koltuğu erişilebilir (ACCESSIBLE): girişe en yakın, basamaksız varsayılan yer.
 * Koltuk numaraları her sırada soldan sağa 1'den başlar, koridor ve eksik köşeler numara almaz.
 */
public final class OrnekDuzenUretici {

    private static final int PERDE_SATIRI = 0;
    private static final int ILK_KOLTUK_SATIRI = 2;
    private static final int KENAR_SUTUNLARI = 2;
    private static final int UST_VE_ALT_SATIRLAR = 3;
    private static final int ERISILEBILIR_KOLTUK_SAYISI = 2;
    private static final char ILK_SIRA_HARFI = 'A';

    private OrnekDuzenUretici() {
    }

    public static DuzenIstegi uret(DuzenSablonu sablon) {
        int sutunSayisi = sablon.siradakiKoltuk() + sablon.koridorlar().size() + KENAR_SUTUNLARI;
        int satirSayisi = sablon.siraSayisi() + UST_VE_ALT_SATIRLAR;
        List<HucreIstegi> hucreler = new ArrayList<>();
        for (int sutun = 1; sutun < sutunSayisi - 1; sutun++) {
            hucreler.add(new HucreIstegi(PERDE_SATIRI, sutun, HucreTipi.SCREEN, null, null, null));
        }
        for (int sira = 0; sira < sablon.siraSayisi(); sira++) {
            hucreler.addAll(sira(sablon, sira));
        }
        int arkaKoridor = satirSayisi - 1;
        int sol = 0;
        int sag = sutunSayisi - 1;
        hucreler.add(new HucreIstegi(arkaKoridor, sablon.girisSolda() ? sol : sag, HucreTipi.ENTRANCE, null, null, null));
        hucreler.add(new HucreIstegi(arkaKoridor, sablon.girisSolda() ? sag : sol, HucreTipi.EXIT, null, null, null));
        return new DuzenIstegi(satirSayisi, sutunSayisi, hucreler);
    }

    private static List<HucreIstegi> sira(DuzenSablonu sablon, int sira) {
        List<HucreIstegi> koltuklar = new ArrayList<>();
        String etiket = String.valueOf((char) (ILK_SIRA_HARFI + sira));
        boolean koseEksik = sira < sablon.eksikKoseSirasi();
        boolean arkaSira = sira == sablon.siraSayisi() - 1;
        int sutun = 1;
        int numara = 1;
        for (int koltuk = 1; koltuk <= sablon.siradakiKoltuk(); koltuk++) {
            boolean uctaki = koltuk == 1 || koltuk == sablon.siradakiKoltuk();
            if (!(koseEksik && uctaki)) {
                KoltukTipi tip = arkaSira && numara <= ERISILEBILIR_KOLTUK_SAYISI
                        ? KoltukTipi.ACCESSIBLE : KoltukTipi.STANDARD;
                koltuklar.add(new HucreIstegi(ILK_KOLTUK_SATIRI + sira, sutun, HucreTipi.SEAT, etiket, numara++, tip));
            }
            sutun += sablon.koridorlar().contains(koltuk) ? 2 : 1;
        }
        return koltuklar;
    }
}
