import { ComponentFixture, discardPeriodicTasks, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { SunucuSaati } from '../../../core/zaman/sunucu-saati.service';
import { GeriSayimComponent } from './geri-sayim.component';

describe('GeriSayimComponent', () => {
  let fixture: ComponentFixture<GeriSayimComponent>;
  let simdi: number;

  beforeEach(() => {
    simdi = Date.parse('2026-10-31T10:00:00Z');
    TestBed.configureTestingModule({
      declarations: [GeriSayimComponent],
      providers: [{ provide: SunucuSaati, useValue: { simdi: () => simdi } }],
    });
    fixture = TestBed.createComponent(GeriSayimComponent);
  });

  function metin(): string {
    return fixture.nativeElement.querySelector('[role="timer"]').textContent.trim();
  }

  function ilerle(ms: number): void {
    simdi += ms;
    tick(ms);
    fixture.detectChanges();
  }

  it('should count down by server time and emit once at zero', fakeAsync(() => {
    let bitisSayisi = 0;
    fixture.componentInstance.bitti.subscribe(() => bitisSayisi++);
    fixture.componentInstance.bitis = '2026-10-31T10:03:00Z';
    fixture.detectChanges();
    ilerle(0);
    expect(metin()).toBe('3:00');

    ilerle(61_000);
    expect(metin()).toBe('1:59');
    expect(fixture.nativeElement.querySelector('[role="timer"]').classList).not.toContain('text-danger');

    ilerle(100_000);
    expect(metin()).toBe('0:19');
    expect(fixture.nativeElement.querySelector('[role="timer"]').classList).toContain('text-danger');

    ilerle(30_000);
    ilerle(5_000);
    expect(metin()).toBe('0:00');
    expect(bitisSayisi).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Süre doldu');
    discardPeriodicTasks();
  }));

  it('should announce minutes rather than every second', fakeAsync(() => {
    fixture.componentInstance.bitis = '2026-10-31T10:03:00Z';
    fixture.detectChanges();
    ilerle(0);

    expect(fixture.nativeElement.querySelector('[aria-live]').textContent).toBe('Kalan süre yaklaşık 3 dakika');
    ilerle(1000);
    expect(fixture.nativeElement.querySelector('[aria-live]').textContent).toBe('Kalan süre yaklaşık 3 dakika');
    fixture.destroy();
    discardPeriodicTasks();
  }));
});
