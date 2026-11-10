import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { BROWSER_STORAGE } from '../storage/safe-storage';
import { LanguageService, RELOAD_PAGE } from './language.service';
import { TranslatePipe } from './translate.pipe';

@Component({
  imports: [TranslatePipe],
  template: `<span>{{ 'header.nav.rankings' | translate }}</span>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
class Host {}

describe('TranslatePipe', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      // 記憶できない状態にして、読み込み直しなしで言語が変わる経路を使う
      providers: [
        { provide: BROWSER_STORAGE, useValue: null },
        { provide: RELOAD_PAGE, useValue: () => undefined },
      ],
    });
  });

  it('renders the text of the current language and follows a language change', async () => {
    const fixture = TestBed.createComponent(Host);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toBe('인기·트렌드');

    TestBed.inject(LanguageService).switchTo('ja');
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toBe('人気・トレンド');
  });
});
