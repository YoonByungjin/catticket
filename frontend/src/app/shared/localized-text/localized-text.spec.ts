import { localizedDisplay } from './localized-text';

describe('localizedDisplay (02 3.4, P-06)', () => {
  const translated = { value: 'オペラ座の怪人', original: '오페라의 유령', translated: true };
  const untranslated = { value: '오페라의 유령', original: '오페라의 유령', translated: false };

  it('shows the original only in Korean', () => {
    expect(localizedDisplay(untranslated, 'ko', true)).toEqual({
      value: '오페라의 유령',
      original: null,
      untranslated: false,
    });
  });

  it('shows the translation with the original below in Japanese (F-11 ②)', () => {
    expect(localizedDisplay(translated, 'ja', true)).toEqual({
      value: 'オペラ座の怪人',
      original: '오페라의 유령',
      untranslated: false,
    });
  });

  it('shows only the translation for items without the original (F-11 ③)', () => {
    expect(localizedDisplay(translated, 'ja', false).original).toBeNull();
  });

  it('shows the original once with the "original" mark when not translated (P-06)', () => {
    expect(localizedDisplay(untranslated, 'ja', true)).toEqual({
      value: '오페라의 유령',
      original: null,
      untranslated: true,
    });
  });
});
