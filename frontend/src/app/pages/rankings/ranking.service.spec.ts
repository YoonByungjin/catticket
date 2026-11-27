import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RankingService } from './ranking.service';
import { RankingResponse } from './ranking.types';

describe('RankingService (API-20)', () => {
  let service: RankingService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(RankingService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('calls GET /api/rankings with period, genre and lang', () => {
    const body: RankingResponse = {
      periodType: 'WEEKLY',
      genre: 'GGGA',
      period: { start: '2026-10-26', end: '2026-11-01' },
      items: [],
    };
    let result: RankingResponse | undefined;
    service
      .getRanking({ period: 'weekly', genre: 'GGGA', lang: 'ja' })
      .subscribe((r) => (result = r));

    const req = http.expectOne(
      (r) =>
        r.url === '/api/rankings' &&
        r.params.get('period') === 'weekly' &&
        r.params.get('genre') === 'GGGA' &&
        r.params.get('lang') === 'ja',
    );
    expect(req.request.method).toBe('GET');
    req.flush(body);
    expect(result).toEqual(body);
  });
});
