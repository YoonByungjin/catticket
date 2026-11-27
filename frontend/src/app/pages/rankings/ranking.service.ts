import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { RankingQuery, RankingResponse } from './ranking.types';

/** 人気順位の API（03 の 8.4） */
@Injectable({ providedIn: 'root' })
export class RankingService {
  private readonly http = inject(HttpClient);

  /** API-20 GET /api/rankings */
  getRanking(query: RankingQuery): Observable<RankingResponse> {
    const params = new HttpParams()
      .set('period', query.period)
      .set('genre', query.genre)
      .set('lang', query.lang);
    return this.http.get<RankingResponse>('/api/rankings', { params });
  }
}
