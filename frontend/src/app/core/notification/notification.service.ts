import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LanguageService } from '../i18n/language.service';
import { MessageKey } from '../i18n/messages';

/** 通知を自動で閉じるまでの時間。02 の 3.3「短く出す」 */
export const NOTIFICATION_DURATION_MS = 4000;

/**
 * CM-03 通知（02 の 3.3、07 の 3.）。操作の結果を画面の下の中央に短く出す。
 * 文言は辞書のキーで受け取り、直書きの文字列を通知に出さない。
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);
  private readonly language = inject(LanguageService);

  show(key: MessageKey, params?: Readonly<Record<string, string | number>>): void {
    this.snackBar.open(this.language.t(key, params), undefined, {
      duration: NOTIFICATION_DURATION_MS,
    });
  }
}
