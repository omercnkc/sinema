package com.sinema.booking.katalog.istemci;

import java.util.List;

/** catalog'a "bu hücreler bu seansın koltukları mı?" sorusu (catalog KoltukCozumlemeIstegi). */
public record KoltukCozumlemeIstegi(List<Long> cellIds) {
}
