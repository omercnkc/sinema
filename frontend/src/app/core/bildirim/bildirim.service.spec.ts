import { Bildirim, BildirimService } from './bildirim.service';

describe('BildirimService', () => {
  let servis: BildirimService;
  let son: readonly Bildirim[] = [];

  beforeEach(() => {
    servis = new BildirimService();
    servis.bildirimler$.subscribe((liste) => (son = liste));
  });

  it('should add notifications with distinct ids and remove by id', () => {
    servis.basari('Kaydedildi');
    servis.hata('Olmadı');

    expect(son.map((bildirim) => bildirim.tur)).toEqual(['basari', 'hata']);
    servis.kaldir(son[0].kimlik);
    expect(son.map((bildirim) => bildirim.mesaj)).toEqual(['Olmadı']);
  });

  it('should not stack the same message twice while it is visible', () => {
    servis.hata('Sunucuya ulaşılamıyor.');
    servis.hata('Sunucuya ulaşılamıyor.');

    expect(son.length).toBe(1);
  });
});
