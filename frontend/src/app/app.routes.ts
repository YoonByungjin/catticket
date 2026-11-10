import { Routes } from '@angular/router';
import { ErrorPage } from './pages/error/error-page';
import { PlaceholderPage } from './pages/placeholder/placeholder-page';

/**
 * 02 の 2. 画面一覧の URL。中身は Wave 2 の各 Issue で PlaceholderPage から置き換える。
 * ログインが必要な画面・管理者の画面の判定（02 の 3.5）も Wave 2 の認証の Issue で足す。
 */
export const routes: Routes = [
  { path: '', pathMatch: 'full', component: PlaceholderPage, data: { screenId: 'SC-01' } },
  { path: 'performances/:performanceId', component: PlaceholderPage, data: { screenId: 'SC-02' } },
  { path: 'rankings', component: PlaceholderPage, data: { screenId: 'SC-03' } },
  { path: 'login', component: PlaceholderPage, data: { screenId: 'SC-04' } },
  { path: 'signup', component: PlaceholderPage, data: { screenId: 'SC-05' } },
  { path: 'signup/verify', component: PlaceholderPage, data: { screenId: 'SC-06' } },
  { path: 'password-reset', component: PlaceholderPage, data: { screenId: 'SC-07' } },
  { path: 'password-reset/confirm', component: PlaceholderPage, data: { screenId: 'SC-08' } },
  { path: 'unlock', component: PlaceholderPage, data: { screenId: 'SC-09' } },
  { path: 'unlock/confirm', component: PlaceholderPage, data: { screenId: 'SC-10' } },
  { path: 'favorites', component: PlaceholderPage, data: { screenId: 'SC-11' } },
  { path: 'account', component: PlaceholderPage, data: { screenId: 'SC-12' } },
  { path: 'admin/performances', component: PlaceholderPage, data: { screenId: 'SC-13' } },
  {
    path: 'admin/performances/:performanceId',
    component: PlaceholderPage,
    data: { screenId: 'SC-14' },
  },
  // SC-99: 上にない URL（02 の 2.）
  { path: '**', component: ErrorPage, data: { reason: 'page-not-found' } },
];
