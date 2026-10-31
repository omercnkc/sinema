import { FormControl, FormGroup, Validators } from '@angular/forms';
import { UygulamaHatasi } from '../../core/hata/uygulama-hatasi';
import { AlanHatasiPipe } from './alan-hatasi.pipe';
import { formHatasiniIsle } from './sunucu-hatalari';

describe('form error helpers', () => {
  let form: FormGroup;
  const pipe = new AlanHatasiPipe();

  beforeEach(() => {
    form = new FormGroup({
      email: new FormControl('ayse@ornek.com', Validators.required),
      phone: new FormControl('05321234567'),
    });
  });

  it('should put server field errors under matching fields and clear them on edit', () => {
    const hata = new UygulamaHatasi(400, 'VALIDATION_FAILED', 'Formu düzeltin.', [
      { field: 'phone', message: 'Geçerli bir cep telefonu girin.' },
    ]);

    expect(formHatasiniIsle(form, hata)).toBeNull();
    const telefon = form.controls['phone'];
    expect(pipe.transform(telefon, 'Telefon')).toBe('Geçerli bir cep telefonu girin.');

    telefon.setValue('05321234568');
    expect(pipe.transform(telefon, 'Telefon')).toBe('');
  });

  it('should map business codes to fields', () => {
    const hata = new UygulamaHatasi(409, 'EMAIL_ALREADY_REGISTERED', 'Bu e-posta adresi zaten kayıtlı.');

    expect(formHatasiniIsle(form, hata, { EMAIL_ALREADY_REGISTERED: 'email' })).toBeNull();
    expect(pipe.transform(form.controls['email'], 'E-posta')).toBe('Bu e-posta adresi zaten kayıtlı.');
  });

  it('should return page message for errors without a field', () => {
    const kilit = new UygulamaHatasi(429, 'ACCOUNT_LOCKED', 'Hesap kilitlendi. 5 dakika sonra tekrar deneyin.');

    expect(formHatasiniIsle(form, kilit)).toBe('Hesap kilitlendi. 5 dakika sonra tekrar deneyin.');
    expect(formHatasiniIsle(form, new Error('beklenmeyen'))).toContain('Beklenmeyen');
  });

  it('should show nothing until field is touched, then first error in Turkish', () => {
    const eposta = form.controls['email'];
    eposta.setValue('');
    expect(pipe.transform(eposta, 'E-posta')).toBe('');

    eposta.markAsTouched();
    expect(pipe.transform(eposta, 'E-posta')).toBe('E-posta zorunludur.');
  });
});
