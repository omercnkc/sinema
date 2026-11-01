import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HaritaHucresi, haritaKur } from './koltuk-haritasi.models';
import { ornekDuzen } from './koltuk-haritasi.models.spec';
import { KoltukHaritasiComponent } from './koltuk-haritasi.component';

describe('KoltukHaritasiComponent', () => {
  let fixture: ComponentFixture<KoltukHaritasiComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ declarations: [KoltukHaritasiComponent] });
    fixture = TestBed.createComponent(KoltukHaritasiComponent);
    fixture.componentInstance.gorunum = haritaKur(ornekDuzen(), new Map([[11, 'SATILDI']]), new Set());
    fixture.detectChanges();
  });

  function koltuk(id: number): HTMLButtonElement {
    return fixture.nativeElement.querySelector(`[data-hucre="${id}"]`);
  }

  function tus(id: number, key: string): void {
    koltuk(id).dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true }));
    fixture.detectChanges();
  }

  it('should place seats on the grid with row labels column and screen band', () => {
    const harita: HTMLElement = fixture.nativeElement.querySelector('.koltuk-haritasi');
    expect(harita.style.gridTemplateColumns).toBe('repeat(5, var(--hucre-boyu))');
    expect(koltuk(10).style.gridRow).toContain('2');
    expect(koltuk(10).style.gridColumn).toContain('2');
    const perde: HTMLElement = fixture.nativeElement.querySelector('.koltuk-haritasi__perde');
    expect(perde.style.gridColumnStart).toBe('2');
    expect(perde.style.gridColumnEnd).toBe('6');
  });

  it('should describe seats for screen readers and mark unavailable ones', () => {
    expect(koltuk(10).getAttribute('aria-label')).toBe('A sırası 1 numara, boş');
    expect(koltuk(11).getAttribute('aria-disabled')).toBe('true');
    expect(koltuk(11).classList).toContain('koltuk--satildi');
    expect(koltuk(20).classList).toContain('koltuk--engelli');
  });

  it('should emit clicked seat with its state', () => {
    const tiklananlar: string[] = [];
    fixture.componentInstance.koltukTiklandi.subscribe((h: HaritaHucresi) => tiklananlar.push(`${h.hucre.cellId}:${h.durum}`));

    koltuk(10).click();
    koltuk(11).click();

    expect(tiklananlar).toEqual(['10:BOS', '11:SATILDI']);
  });

  it('should have a single tab stop and move with arrow keys', () => {
    const odaklananlar = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('button.koltuk'))
      .filter((b) => b.tabIndex === 0);
    expect(odaklananlar.map((b) => b.dataset['hucre'])).toEqual(['10']);

    tus(10, 'ArrowRight');
    expect(koltuk(11).tabIndex).toBe(0);
    expect(document.activeElement).toBe(koltuk(11));

    tus(11, 'ArrowDown');
    expect(koltuk(20).tabIndex).toBe(0);

    tus(20, 'ArrowDown');
    expect(koltuk(20).tabIndex).toBe(0);
  });
});
