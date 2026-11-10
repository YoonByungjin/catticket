import { Pipe, PipeTransform, inject } from '@angular/core';
import { LanguageService } from './language.service';
import { MessageKey } from './messages';

/**
 * テンプレートで辞書の文言を出す: `{{ 'header.login' | translate }}`
 * 言語の signal を読むため pure にしない（pure だと言語が変わっても再計算されない）。
 */
@Pipe({ name: 'translate', pure: false })
export class TranslatePipe implements PipeTransform {
  private readonly language = inject(LanguageService);

  transform(key: MessageKey, params?: Readonly<Record<string, string | number>>): string {
    return this.language.t(key, params);
  }
}
