import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbToastModule } from '@ng-bootstrap/ng-bootstrap';
import { BildirimService } from '../../core/bildirim/bildirim.service';
import { BildirimAlaniComponent } from './bildirim-alani.component';

describe('BildirimAlaniComponent', () => {
  let fixture: ComponentFixture<BildirimAlaniComponent>;
  let servis: BildirimService;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [NgbToastModule], declarations: [BildirimAlaniComponent] });
    servis = TestBed.inject(BildirimService);
    fixture = TestBed.createComponent(BildirimAlaniComponent);
  });

  it('should render each notification with its color', () => {
    servis.hata('Sunucuya ulaşılamıyor.');
    servis.basari('Ödeme alındı.');
    fixture.detectChanges();

    const toastlar = Array.from<HTMLElement>(fixture.nativeElement.querySelectorAll('ngb-toast'));
    expect(toastlar.map((toast) => toast.textContent?.trim())).toEqual(['Sunucuya ulaşılamıyor.', 'Ödeme alındı.']);
    expect(toastlar[0].classList).toContain('bg-danger');
    expect(toastlar[1].classList).toContain('bg-success');
  });
});
