import { CoreModule } from './core.module';

describe('CoreModule', () => {
  it('should refuse a second import', () => {
    const ilk = new CoreModule(null);
    expect(() => new CoreModule(ilk)).toThrowError(/yalnızca AppModule/);
  });
});
