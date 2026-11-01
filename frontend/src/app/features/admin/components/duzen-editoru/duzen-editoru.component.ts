import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, HostListener, Input, Output } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import {
  boya, boyutDegistir, dogrula, EditorDuzeni, Firca, IZGARA_EN_AZ, IZGARA_EN_FAZLA, koltukEtiketleri, koltukSayilari,
  KoltukEtiketi, konumAnahtari, silinecekHucreSayisi, EditorHucresi,
} from '../../models/duzen-editoru.models';

interface FircaSecenegi {
  readonly firca: Firca;
  readonly ad: string;
  /** Örnek kutucuğun sınıfı (hücre görünümüyle aynı). */
  readonly sinif: string;
}

interface HucreGorunumu {
  readonly anahtar: string;
  readonly satir: number;
  readonly sutun: number;
  readonly sinif: string;
  readonly metin: string;
  readonly erisilebilirEtiket: string;
}

interface SatirGorunumu {
  readonly satir: number;
  /** Satırdaki koltukların sıra harfi (koltuksuz satırda boş). */
  readonly sira: string;
  readonly hucreler: readonly HucreGorunumu[];
}

const FIRCALAR: readonly FircaSecenegi[] = [
  { firca: 'KOLTUK', ad: 'Koltuk', sinif: 'hucre--koltuk' },
  { firca: 'ENGELLI_KOLTUK', ad: 'Engelli koltuğu', sinif: 'hucre--koltuk hucre--engelli' },
  { firca: 'PERDE', ad: 'Perde', sinif: 'hucre--perde' },
  { firca: 'GIRIS', ad: 'Giriş', sinif: 'hucre--giris' },
  { firca: 'CIKIS', ad: 'Çıkış', sinif: 'hucre--cikis' },
  { firca: 'DUVAR', ad: 'Duvar', sinif: 'hucre--duvar' },
  { firca: 'SIL', ad: 'Sil (boşluk/koridor)', sinif: '' },
];

const YONLER: Readonly<Record<string, readonly [number, number]>> = {
  ArrowUp: [-1, 0],
  ArrowDown: [1, 0],
  ArrowLeft: [0, -1],
  ArrowRight: [0, 1],
};

/** Klavyeyle (Enter/Boşluk) gelen click'te `detail` 0'dır; fare tıklaması zaten pointerdown'da boyanmıştır. */
const KLAVYE_TIKLAMASI = 0;

/**
 * Salon düzeni editörü (dumb, kontrollü): düzeni `duzen` ile alır, her değişikliği yeni düzen olarak
 * `duzenDegisti` ile verir; servis çağırmaz. Fırça seçilir, hücreler tıklanarak ya da basılı tutup sürükleyerek
 * boyanır (fare ve dokunmatik aynı pointer olaylarıyla). Klavyede ızgaraya tek sekme durağıyla girilir, ok
 * tuşlarıyla gezilir, Enter/Boşluk odaktaki hücreyi boyar. Geri alma yok (K41).
 */
@Component({
  selector: 'app-duzen-editoru',
  templateUrl: './duzen-editoru.component.html',
  styleUrls: ['./duzen-editoru.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DuzenEditoruComponent {
  @Output() readonly duzenDegisti = new EventEmitter<EditorDuzeni>();

  readonly fircalar = FIRCALAR;
  readonly enAz = IZGARA_EN_AZ;
  readonly enFazla = IZGARA_EN_FAZLA;
  readonly boyutKurallari = [Validators.required, Validators.min(IZGARA_EN_AZ), Validators.max(IZGARA_EN_FAZLA)];
  readonly satirKontrolu = new FormControl(null, this.boyutKurallari);
  readonly sutunKontrolu = new FormControl(null, this.boyutKurallari);
  /** Form grubu gerekli: [formGroup] olmayan <form> tarayıcının kendi gönderimini yapar (sayfa yenilenir). */
  readonly boyutFormu = new FormGroup({ satir: this.satirKontrolu, sutun: this.sutunKontrolu });

  firca: Firca = 'KOLTUK';
  satirlar: readonly SatirGorunumu[] = [];
  sorunlar: readonly string[] = [];
  koltukToplami = 0;
  engelliToplami = 0;
  odakAnahtari = konumAnahtari(0, 0);

  private duzenVerisi: EditorDuzeni | null = null;
  private boyaniyor = false;
  private sonBoyananAnahtar: string | null = null;

  constructor(private readonly eleman: ElementRef<HTMLElement>) {}

  @Input() set duzen(duzen: EditorDuzeni) {
    // Kendi verdiğimiz düzen geri geliyorsa (kapsayıcı bağlamayı güncelledi) görünüm zaten günceldir
    if (duzen === this.duzenVerisi) {
      return;
    }
    const boyutDegisti = duzen.satirSayisi !== this.duzenVerisi?.satirSayisi || duzen.sutunSayisi !== this.duzenVerisi?.sutunSayisi;
    this.uygula(duzen);
    if (boyutDegisti) {
      this.satirKontrolu.setValue(duzen.satirSayisi);
      this.sutunKontrolu.setValue(duzen.sutunSayisi);
    }
  }

  /** Boyut küçülürse silinecek hücre sayısı (düğmede gösterilir; geri alma yok). */
  get silinecekHucre(): number {
    return this.duzenVerisi && this.boyutGecerli
      ? silinecekHucreSayisi(this.duzenVerisi, Number(this.satirKontrolu.value), Number(this.sutunKontrolu.value))
      : 0;
  }

  get boyutGecerli(): boolean {
    return this.satirKontrolu.valid && this.sutunKontrolu.valid;
  }

  fircaSec(firca: Firca): void {
    this.firca = firca;
  }

  boyutuUygula(): void {
    if (!this.duzenVerisi || !this.boyutGecerli) {
      return;
    }
    this.degistir(boyutDegistir(this.duzenVerisi, Number(this.satirKontrolu.value), Number(this.sutunKontrolu.value)));
  }

  boyamaBasladi(olay: PointerEvent): void {
    const hucre = (olay.target as Element).closest<HTMLElement>('[data-anahtar]');
    if (olay.button !== 0 || !hucre) {
      return;
    }
    // Tarayıcının metin seçimi/sürükle-bırak davranışı boyamayı bölmesin
    olay.preventDefault();
    this.boyaniyor = true;
    this.hucreyiBoya(hucre);
  }

  /** Dokunmatikte olaylar ilk hücreye kilitli kaldığından hedef, parmağın altındaki elemandan bulunur. */
  boyamaSurdu(olay: PointerEvent): void {
    if (!this.boyaniyor) {
      return;
    }
    const hucre = document.elementFromPoint(olay.clientX, olay.clientY)?.closest<HTMLElement>('[data-anahtar]');
    if (hucre && this.eleman.nativeElement.contains(hucre)) {
      this.hucreyiBoya(hucre);
    }
  }

  /** Bırakma ızgaranın dışında olsa da boyama biter. */
  @HostListener('window:pointerup')
  @HostListener('window:pointercancel')
  boyamaBitti(): void {
    this.boyaniyor = false;
    this.sonBoyananAnahtar = null;
  }

  klavyeylaBoya(olay: MouseEvent, hucre: HucreGorunumu): void {
    if (olay.detail === KLAVYE_TIKLAMASI && this.duzenVerisi) {
      this.odakAnahtari = hucre.anahtar;
      this.degistir(boya(this.duzenVerisi, hucre.satir, hucre.sutun, this.firca));
    }
  }

  tusaBasildi(olay: KeyboardEvent, hucre: HucreGorunumu): void {
    const yon = YONLER[olay.key];
    if (!yon || !this.duzenVerisi) {
      return;
    }
    olay.preventDefault();
    const satir = Math.min(this.duzenVerisi.satirSayisi - 1, Math.max(0, hucre.satir + yon[0]));
    const sutun = Math.min(this.duzenVerisi.sutunSayisi - 1, Math.max(0, hucre.sutun + yon[1]));
    this.odakAnahtari = konumAnahtari(satir, sutun);
    this.eleman.nativeElement.querySelector<HTMLElement>(`[data-anahtar="${this.odakAnahtari}"]`)?.focus();
  }

  fircaIzle(_: number, secenek: FircaSecenegi): Firca {
    return secenek.firca;
  }

  satirIzle(_: number, satir: SatirGorunumu): number {
    return satir.satir;
  }

  hucreIzle(_: number, hucre: HucreGorunumu): string {
    return hucre.anahtar;
  }

  sorunIzle(_: number, sorun: string): string {
    return sorun;
  }

  private hucreyiBoya(eleman: HTMLElement): void {
    const anahtar = eleman.dataset['anahtar'];
    if (!anahtar || anahtar === this.sonBoyananAnahtar || !this.duzenVerisi) {
      return;
    }
    this.sonBoyananAnahtar = anahtar;
    this.odakAnahtari = anahtar;
    const [satir, sutun] = anahtar.split(',').map(Number);
    this.degistir(boya(this.duzenVerisi, satir, sutun, this.firca));
  }

  /** Görünüm hemen güncellenir (sürüklerken kapsayıcının bağlamayı yenilemesini beklemez), sonra dışarı bildirilir. */
  private degistir(yeni: EditorDuzeni): void {
    if (yeni === this.duzenVerisi) {
      return;
    }
    this.uygula(yeni);
    this.duzenDegisti.emit(yeni);
  }

  private uygula(duzen: EditorDuzeni): void {
    this.duzenVerisi = duzen;
    this.satirlar = satirGorunumleri(duzen);
    this.sorunlar = dogrula(duzen);
    const sayilar = koltukSayilari(duzen);
    this.koltukToplami = sayilar.toplam;
    this.engelliToplami = sayilar.engelli;
    // Izgara küçüldüyse odak dışarıda kalmış olabilir
    if (!this.izgaradaMi(this.odakAnahtari, duzen)) {
      this.odakAnahtari = konumAnahtari(0, 0);
    }
  }

  private izgaradaMi(anahtar: string, duzen: EditorDuzeni): boolean {
    const [satir, sutun] = anahtar.split(',').map(Number);
    return satir < duzen.satirSayisi && sutun < duzen.sutunSayisi;
  }
}

function satirGorunumleri(duzen: EditorDuzeni): SatirGorunumu[] {
  const etiketler = koltukEtiketleri(duzen);
  return Array.from({ length: duzen.satirSayisi }, (_, satir) => {
    const hucreler = Array.from({ length: duzen.sutunSayisi }, (__, sutun) => {
      const anahtar = konumAnahtari(satir, sutun);
      return hucreGorunumu(anahtar, satir, sutun, duzen.hucreler.get(anahtar), etiketler.get(anahtar));
    });
    const sira = hucreler.map((h) => etiketler.get(h.anahtar)?.sira).find((harf) => harf !== undefined) ?? '';
    return { satir, sira, hucreler };
  });
}

function hucreGorunumu(
  anahtar: string, satir: number, sutun: number, hucre: EditorHucresi | undefined, etiket: KoltukEtiketi | undefined,
): HucreGorunumu {
  const konum = `${satir + 1}. satır, ${sutun + 1}. sütun`;
  if (!hucre) {
    return { anahtar, satir, sutun, sinif: '', metin: '', erisilebilirEtiket: `${konum}: boş` };
  }
  const koltukEtiketi = etiket ? `${etiket.sira}-${etiket.numara}` : '';
  switch (hucre.tip) {
    case 'SEAT':
      return hucre.koltukTipi === 'ACCESSIBLE'
        ? { anahtar, satir, sutun, sinif: 'hucre--koltuk hucre--engelli', metin: '♿', erisilebilirEtiket: `${konum}: engelli koltuğu ${koltukEtiketi}` }
        : { anahtar, satir, sutun, sinif: 'hucre--koltuk', metin: String(etiket?.numara ?? ''), erisilebilirEtiket: `${konum}: koltuk ${koltukEtiketi}` };
    case 'SCREEN':
      return { anahtar, satir, sutun, sinif: 'hucre--perde', metin: '', erisilebilirEtiket: `${konum}: perde` };
    case 'ENTRANCE':
      return { anahtar, satir, sutun, sinif: 'hucre--giris', metin: 'G', erisilebilirEtiket: `${konum}: giriş` };
    case 'EXIT':
      return { anahtar, satir, sutun, sinif: 'hucre--cikis', metin: 'Ç', erisilebilirEtiket: `${konum}: çıkış` };
    case 'WALL':
      return { anahtar, satir, sutun, sinif: 'hucre--duvar', metin: '', erisilebilirEtiket: `${konum}: duvar` };
  }
}
