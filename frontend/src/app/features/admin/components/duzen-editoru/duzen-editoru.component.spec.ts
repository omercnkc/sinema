import { ChangeDetectorRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SharedModule } from '../../../../shared/shared.module';
import { boya, bosDuzen, EditorDuzeni } from '../../models/duzen-editoru.models';
import { DuzenEditoruComponent } from './duzen-editoru.component';

describe('DuzenEditoruComponent', () => {
  let fixture: ComponentFixture<DuzenEditoruComponent>;
  let bilesen: DuzenEditoruComponent;
  let sonDuzen: EditorDuzeni | null;
  let bildirimSayisi: number;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [SharedModule], declarations: [DuzenEditoruComponent] });
    fixture = TestBed.createComponent(DuzenEditoruComponent);
    bilesen = fixture.componentInstance;
    sonDuzen = null;
    bildirimSayisi = 0;
    bilesen.duzenDegisti.subscribe((duzen: EditorDuzeni) => {
      sonDuzen = duzen;
      bildirimSayisi++;
    });
    bilesen.duzen = bosDuzen(3, 4);
    fixture.detectChanges();
  });

  /** Bileşen OnPush: testte doğrudan yapılan değişiklikler işaretlenince çizilir. */
  function yenile(): void {
    fixture.debugElement.injector.get(ChangeDetectorRef).markForCheck();
    fixture.detectChanges();
  }

  function hucre(satir: number, sutun: number): HTMLButtonElement {
    return fixture.nativeElement.querySelector(`[data-anahtar="${satir},${sutun}"]`);
  }

  function pointer(tur: string, hedef: Element, kaynak: Element = hedef): void {
    const kutu = kaynak.getBoundingClientRect();
    hedef.dispatchEvent(new PointerEvent(tur, {
      bubbles: true, button: 0, clientX: kutu.left + kutu.width / 2, clientY: kutu.top + kutu.height / 2,
    }));
  }

  function fircaSec(ad: string): void {
    const dugmeler = Array.from(fixture.nativeElement.querySelectorAll('[aria-label="Fırça"] button')) as HTMLButtonElement[];
    dugmeler.find((d) => d.textContent?.trim() === ad)?.click();
    yenile();
  }

  it('should draw grid with one tab stop and show validation problems', () => {
    expect(fixture.nativeElement.querySelectorAll('.duzen-editoru__satir').length).toBe(3);
    expect(fixture.nativeElement.querySelectorAll('button.hucre').length).toBe(12);
    expect(fixture.nativeElement.querySelectorAll('button.hucre[tabindex="0"]').length).toBe(1);
    expect(fixture.nativeElement.querySelectorAll('[data-test="duzen-sorunlari"] li').length).toBe(2);
    expect((fixture.nativeElement.querySelector('#duzen-satir') as HTMLInputElement).value).toBe('3');
  });

  it('should paint by pressing and dragging across cells, once per cell', () => {
    pointer('pointerdown', hucre(1, 0));
    pointer('pointermove', hucre(1, 0), hucre(1, 1));
    pointer('pointermove', hucre(1, 0), hucre(1, 1));
    pointer('pointermove', hucre(1, 0), hucre(1, 2));
    window.dispatchEvent(new PointerEvent('pointerup'));
    pointer('pointermove', hucre(1, 0), hucre(1, 3));
    yenile();

    expect(bildirimSayisi).toBe(3);
    expect(Array.from(sonDuzen?.hucreler.keys() ?? [])).toEqual(['1,0', '1,1', '1,2']);
    expect(hucre(1, 2).classList).toContain('hucre--koltuk');
    expect(hucre(1, 0).getAttribute('aria-label')).toBe('2. satır, 1. sütun: koltuk A-1');
    expect(fixture.nativeElement.querySelector('.duzen-editoru__sira').textContent.trim()).toBe('');
    expect(fixture.nativeElement.querySelectorAll('.duzen-editoru__sira')[1].textContent.trim()).toBe('A');
  });

  it('should use selected brush and paint with keyboard after moving with arrows', () => {
    fircaSec('Perde');
    hucre(0, 0).focus();
    hucre(0, 0).dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    yenile();
    expect(document.activeElement).toBe(hucre(0, 1));

    hucre(0, 1).click();
    yenile();

    expect(sonDuzen?.hucreler.get('0,1')?.tip).toBe('SCREEN');
    expect(hucre(0, 1).getAttribute('tabindex')).toBe('0');
  });

  it('should warn how many cells resizing drops, then apply', () => {
    bilesen.duzen = boya(boya(bosDuzen(3, 4), 2, 3, 'KOLTUK'), 0, 0, 'PERDE');
    yenile();
    bilesen.satirKontrolu.setValue(2);
    yenile();

    const dugme = fixture.nativeElement.querySelector('form[aria-label="Izgara boyutu"] button') as HTMLButtonElement;
    expect(dugme.textContent).toContain('(1 hücre silinir)');

    dugme.click();
    yenile();

    expect(sonDuzen?.satirSayisi).toBe(2);
    expect(sonDuzen?.hucreler.size).toBe(1);
    expect(fixture.nativeElement.querySelectorAll('.duzen-editoru__satir').length).toBe(2);
  });

  it('should reject out of range size', () => {
    bilesen.sutunKontrolu.setValue(51);
    yenile();

    expect((fixture.nativeElement.querySelector('form[aria-label="Izgara boyutu"] button') as HTMLButtonElement).disabled).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('1–50 arasında olmalı');
  });
});
