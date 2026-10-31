import { SekmeGorunurlugu } from './sekme-gorunurlugu';

describe('SekmeGorunurlugu', () => {
  it('should emit current state and changes on visibilitychange', () => {
    const belge = new EventTarget() as Document & { visibilityState: DocumentVisibilityState };
    Object.defineProperty(belge, 'visibilityState', { value: 'visible', writable: true });
    const durumlar: boolean[] = [];

    new SekmeGorunurlugu(belge).gorunur$.subscribe((gorunur) => durumlar.push(gorunur));
    belge.visibilityState = 'hidden';
    belge.dispatchEvent(new Event('visibilitychange'));
    belge.dispatchEvent(new Event('visibilitychange'));
    belge.visibilityState = 'visible';
    belge.dispatchEvent(new Event('visibilitychange'));

    expect(durumlar).toEqual([true, false, true]);
  });
});
