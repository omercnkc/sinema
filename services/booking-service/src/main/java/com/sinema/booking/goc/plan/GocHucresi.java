package com.sinema.booking.goc.plan;

/**
 * Göç planlamasında bir düzen hücresi (catalog'un hücre cevabından). Yalnızca koltuk ve perde hücreleri anlamlıdır;
 * diğer tipler (giriş, çıkış, duvar) planlamayı etkilemez.
 *
 * @param etiket koltukta biletteki biçim ("F-7"), diğerlerinde boş
 */
public record GocHucresi(long hucreId, int satir, int sutun, boolean koltuk, boolean perde, String etiket,
        boolean engelli) {

    public static GocHucresi koltuk(long hucreId, int satir, int sutun, String etiket, boolean engelli) {
        return new GocHucresi(hucreId, satir, sutun, true, false, etiket, engelli);
    }

    public static GocHucresi perde(long hucreId, int satir, int sutun) {
        return new GocHucresi(hucreId, satir, sutun, false, true, null, false);
    }

    boolean ayniKonumda(GocHucresi diger) {
        return satir == diger.satir && sutun == diger.sutun;
    }

    double uzaklik(GocHucresi diger) {
        return Math.hypot(satir - diger.satir, sutun - diger.sutun);
    }
}
