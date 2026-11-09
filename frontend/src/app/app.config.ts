import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { routes } from './app.routes';
import { LanguageService } from './core/i18n/language.service';
import { ThemeService } from './core/theme/theme.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // ルートの data（画面 ID・SC-99 の場合）をコンポーネントの input で受け取る
    provideRouter(routes, withComponentInputBinding()),
    // 03 の 1.3: Cookie XSRF-TOKEN → ヘッダー X-XSRF-TOKEN（Angular の既定と同じ名前だが明示する）
    provideHttpClient(
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' }),
    ),
    provideAppInitializer(() => {
      // ADR 0016 決定3: 読み込まれたフォントからの推測に頼らず Outlined を既定にする
      inject(MatIconRegistry).setDefaultFontSetClass(
        'material-symbols-outlined',
        'mat-ligature-font',
      );
      // 表示言語と表示（ライト/ダーク）を最初の画面の前に <html> へ反映する
      inject(LanguageService);
      inject(ThemeService);
    }),
  ],
};
