import { SayfaYonlendirici } from './sayfa-yonlendirici.service';

describe('SayfaYonlendirici', () => {
  let assign: jasmine.Spy;
  let yonlendirici: SayfaYonlendirici;

  beforeEach(() => {
    assign = jasmine.createSpy('assign');
    yonlendirici = new SayfaYonlendirici({ location: { assign } } as unknown as Document);
  });

  it('should navigate to http(s) addresses', () => {
    expect(yonlendirici.disAdreseGit('https://sandbox-cpp.iyzipay.com?token=abc')).toBeTrue();
    expect(assign).toHaveBeenCalledWith('https://sandbox-cpp.iyzipay.com/?token=abc');
  });

  it('should refuse script and malformed addresses', () => {
    // eslint-disable-next-line no-script-url
    expect(yonlendirici.disAdreseGit('javascript:alert(1)')).toBeFalse();
    expect(yonlendirici.disAdreseGit('data:text/html,<p>x</p>')).toBeFalse();
    expect(yonlendirici.disAdreseGit('/goreli/adres')).toBeFalse();
    expect(assign).not.toHaveBeenCalled();
  });
});
