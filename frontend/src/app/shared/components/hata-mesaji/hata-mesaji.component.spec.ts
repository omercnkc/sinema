import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HataMesajiComponent } from './hata-mesaji.component';

describe('HataMesajiComponent', () => {
  let fixture: ComponentFixture<HataMesajiComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ declarations: [HataMesajiComponent] });
    fixture = TestBed.createComponent(HataMesajiComponent);
    fixture.componentInstance.mesaj = 'Filmler yüklenemedi.';
  });

  it('should show message as alert without retry button by default', () => {
    fixture.detectChanges();
    const kok: HTMLElement = fixture.nativeElement;

    expect(kok.querySelector('[role="alert"]')?.textContent).toContain('Filmler yüklenemedi.');
    expect(kok.querySelector('button')).toBeNull();
  });

  it('should emit retry when button clicked', () => {
    fixture.componentInstance.tekrarDenenebilir = true;
    fixture.detectChanges();
    let tetiklendi = false;
    fixture.componentInstance.tekrarDene.subscribe(() => (tetiklendi = true));

    fixture.nativeElement.querySelector('button').click();

    expect(tetiklendi).toBeTrue();
  });
});
