package com.sinema.catalog.duzen.entity;

/**
 * Düzen hücresinin tipi. Boşluk/koridor için tip yok: hücre hiç saklanmaz (db-planlama §3.2),
 * editörün "boş" fırçası hücreyi silmek anlamına gelir.
 */
public enum HucreTipi {
    SEAT,
    ENTRANCE,
    EXIT,
    SCREEN,
    WALL
}
