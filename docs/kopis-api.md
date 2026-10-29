# KOPIS Open API 정리

> 출처
> - 웹: https://www.kopis.or.kr/por/cs/openapi/openApiList.do (2026-10-29 확인)
> - **OPEN API 개발가이드 v5.0** (서비스 버전 5.0, 배포일 2026-04-23, 32쪽) — 사용자 보관본
>
> 코드값의 기준 문서는 `kopis_openapi_code_v3.7` (공통코드 다운로드). **아직 받지 않음.**

## 이용 조건

- 인증키: kopis.or.kr → 인증키 발급신청 (PC에서만, 1인 1개, 메일로 수신, 양도·공유 금지)
- **출처 표기 필수**. 없으면 서비스 중단될 수 있음
  - 예: `출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)`
- 쿼리 제한 있음 (초과 시 중지). **개발가이드에도 구체적인 수치는 없음** → 호출 간격을 두고, 실패 시 재시도 간격을 늘리는 방식으로 대비
- 응답 형식: XML (REST)

## 보안 주의

- 개발가이드 기준 **전송 암호화 없음(SSL 미지원)**, 요청 URL은 모두 `http://`
- 인증키가 쿼리스트링(`?service=...`)으로 **평문 전송**됨
- 따라서 **인증키는 서버(파이프라인)에서만 사용**하고, 브라우저 JS에서 KOPIS를 직접 호출하지 않는다
- 키는 `.env`의 `KOPIS_API_KEY`에만 보관

## 개발가이드에서 발견한 오류·불일치 (구현 시 주의)

| 위치 | 내용 | 판단 |
|---|---|---|
| 4.1 공연시설상세 개요 | 서비스 URL이 `pblprfr/{공연시설아이디}`로 적혀 있음 | 오타. 4.2 예제와 웹사이트 모두 `prfplc/{mt10id}` |
| 공연목록 `prfstate` | 가이드는 크기 4, 웹사이트는 크기 2 | 값은 `01`~`03` 두 자리로 사용 |
| 공연시설상세 `mt10id` | 공연상세 요청에선 크기 8, 시설상세 요청에선 크기 20 | 문자열로 저장, 길이 여유 있게 |
| 예매상황판 `date` | 필수로 표기됐지만 예제 URL엔 없음 | 실제 호출로 확인 필요 |

---

## 1. 공연목록 `pblprfr`

```
GET http://kopis.or.kr/openApi/restful/pblprfr
```

### 요청 변수

| 변수명 | 필수 | 크기 | 설명 | 샘플 |
|---|---|---|---|---|
| `service` | O | 60 | 발급받은 인증키 | |
| `stdate` | O | 8 | 공연시작일자 | `20230101` |
| `eddate` | O | 8 | 공연종료일자 | `20230630` |
| `cpage` | O | 3 | 현재페이지 | `1` |
| `rows` | O | 3 | 페이지당 목록 수 | `10` (최대 100) |
| `shprfnm` | X | 100 | 공연명 (URL 인코딩) | `사랑` |
| `shprfnmfct` | X | 100 | 공연시설명 (URL 인코딩) | `예술의전당` |
| `shcate` | X | 4 | 장르코드 | `AAAA` |
| `prfplccd` | X | 4 | 공연장코드 | `FC000001-01` |
| `signgucode` | X | 2 | 지역(시도)코드. **서울 = `11`** | `11` |
| `signgucodesub` | X | 4 | 지역(구군)코드 | `1111` |
| `kidstate` | X | 1 | 아동공연여부 (미지정 시 전체) | `Y` |
| `prfstate` | X | 2 | 공연상태코드 | `01` |
| `openrun` | X | 2 | 오픈런 | `Y` |
| `afterdate` | X | 8 | 해당일자 이후 등록/수정된 항목만 | `20230101` |

> `afterdate`는 파이프라인의 **증분 수집**(지난 실행 이후 바뀐 것만 가져오기)에 사용할 수 있음

### 응답 필드

| 필드 | 설명 | 샘플 |
|---|---|---|
| `mt20id` | 공연ID (**중복 방지 키로 사용**) | `PF178134` |
| `prfnm` | 공연명 | `반짝반짝 인어공주` |
| `prfpdfrom` | 공연시작일 (`YYYY.MM.DD`) | `2021.08.21` |
| `prfpdto` | 공연종료일 (`YYYY.MM.DD`) | `2024.09.29` |
| `fcltynm` | 공연시설명 | `달밤엔씨어터` |
| `poster` | 포스터 이미지 URL | |
| `area` | 공연지역 | `서울특별시` |
| `genrenm` | 장르명 | `뮤지컬` |
| `openrun` | 오픈런 | `Y` |
| `prfstate` | 공연상태 | `공연중` |

### 제약 (파이프라인 설계 시 반영)

| 결과코드 | 의미 |
|---|---|
| `00` | 정상 |
| `01` | 요청 파라미터 오류 |
| `02` | 등록되지 않은 인증키 |
| `03` | DB 오류 |
| `04` | 데이터 없음 |
| `05` | **조회 기간은 최대 31일** → 기간을 31일 단위로 나눠 반복 호출 |
| `06` | **최대 100건** → `cpage`를 올려가며 반복 호출 |

### 요청 예시

```
http://www.kopis.or.kr/openApi/restful/pblprfr?service={KEY}&stdate=20230601&eddate=20230630&cpage=1&rows=10&prfstate=02&signgucode=11&signgucodesub=1111&kidstate=Y
```

> 주의: 공연목록 응답에는 **좌표와 공연시설ID가 없음**. 시설ID는 공연상세(2번)에서, 좌표는 공연시설상세(3번)에서 가져와야 함

---

## 2. 공연상세 `pblprfr/{공연ID}`

```
GET http://kopis.or.kr/openApi/restful/pblprfr/{mt20id}?service={KEY}
```

### 요청 변수

| 변수명 | 필수 | 크기 | 설명 | 샘플 |
|---|---|---|---|---|
| `service` | O | 60 | 인증키 | |
| `mt20id` | O | 8 | 공연ID (경로에 넣음, 공연목록에서 얻음) | `PF132236` |

### 응답 필드 (이 프로젝트에 필요한 것 위주)

| 필드 | 설명 | 샘플 |
|---|---|---|
| `mt20id` | 공연ID | `PF132236` |
| **`mt10id`** | **공연시설ID** → 공연시설상세 호출에 사용 | `FC001431` |
| `mt13id` | 공연장ID (시설 안의 홀) | `FC001431-01` |
| `prfnm` | 공연명 | |
| `prfpdfrom` / `prfpdto` | 시작일 / 종료일 (`YYYY.MM.DD`) | |
| `fcltynm` | 공연시설명 | |
| `poster` | 포스터 이미지 URL | |
| `genrenm` | 장르 | `연극` |
| `prfstate` | 공연상태 | `공연중` |
| `pcseguidance` | 티켓가격 | `전석 30,000원` |
| `dtguidance` | 공연시간 | `화요일 ~ 금요일(20:00), ...` |
| `prfruntime` | 런타임 | `1시간 30분` |
| `prfage` | 관람 연령 | `만 12세 이상` |
| `prfcast` / `prfcrew` | 출연진 / 제작진 | |
| `visit` | 내한 공연 여부 | `N` |
| `updatedate` | 최종수정일 | `2019-07-25 10:03:14` |
| **`relates/relate`** | **예매처 목록** (`relatenm` 이름, `relateurl` URL). 이름이 없는 항목도 있음 | `티켓링크` |
| `styurls/styurl` | 소개 이미지 URL 목록 | |

> 공연 1건마다 1번 호출해야 하므로 호출 수가 많음. `afterdate`로 바뀐 공연만 상세 조회하면 호출을 줄일 수 있음

---

## 3. 공연시설상세 `prfplc/{시설ID}`

```
GET http://kopis.or.kr/openApi/restful/prfplc/{mt10id}?service={KEY}
```

### 요청 변수

| 변수명 | 필수 | 크기 | 설명 | 샘플 |
|---|---|---|---|---|
| `service` | O | 60 | 인증키 | |
| `mt10id` | O | 8 | 공연시설ID (경로에 넣음) | `FC001247` |

### 응답 필드 (주요)

| 필드 | 설명 | 샘플 |
|---|---|---|
| `mt10id` | 공연시설ID | `FC001247` |
| `fcltynm` | 공연시설명 | `올림픽공원` |
| `adres` | 주소 | `서울특별시 송파구 올림픽로 424 ...` |
| **`la`** | **위도** | `37.52112` |
| **`lo`** | **경도** | `127.12836360000005` |
| `telno` | 전화번호 | |
| `relateurl` | 홈페이지 | |
| `seatscale` | 객석 수 | |
| `mt13cnt` | 공연장 수 | `11` |
| `mt13s/mt13` | 시설 안의 공연장 목록 (`mt13id`, `prfplcnm`, `seatscale` 등) | `FC001247-01` KSPO DOME |
| `parkinglot`, `restaurant`, `cafe`, `store` | 편의시설 (Y/N) | |
| `elevbarrier`, `runwbarrier`, `restbarrier`, `parkbarrier` | 장애인 편의시설 (Y/N) | |

> 시설 정보는 잘 바뀌지 않으므로 **한 번 받아서 DB에 저장(캐시)** 하고, 새 시설이 나올 때만 호출

### (참고) 공연시설목록 `prfplc`

```
GET http://www.kopis.or.kr/openApi/restful/prfplc?service={KEY}&cpage=1&rows=100&signgucode=11
```

| 요청 변수 | 필수 | 설명 |
|---|---|---|
| `service`, `cpage`, `rows`(최대 100) | O | |
| `shprfnmfct` | X | 공연시설명 |
| `fcltychartr` | X | 공연시설특성코드 |
| `signgucode` / `signgucodesub` | X | 지역(시도) / 지역(구군) |
| `afterdate` | X | 해당일자 이후 등록/수정된 것만 |

응답: `mt10id`, `fcltynm`, `mt13cnt`, `fcltychartr`, `sidonm`, `gugunnm`, `opende` — **좌표는 없음**.
서울 시설 ID를 미리 전부 모아두는 용도로만 쓸 수 있음.

---

## 4. 코드값 (공통코드 파일로 확인 필요)

아래는 알려진 값이며, 실제 구현 전에 공통코드 파일로 확인할 것.

| 구분 | 코드 | 의미 |
|---|---|---|
| `prfstate` | `01` / `02` / `03` | 공연예정 / 공연중 / 공연완료 |
| `shcate` | `AAAA` | 연극 |
| | `GGGA` | 뮤지컬 |
| | `CCCA` | 서양음악(클래식) |
| | `CCCC` | 한국음악(국악) |
| | `CCCD` | 대중음악 |
| | `BBBC` / `BBBE` | 무용 / 대중무용 |
| | `EEEA` / `EEEB` | 복합 / 서커스·마술 |

---

## 5. 이 프로젝트에서 쓸 조합 (계획)

```
공연목록 pblprfr          서울(signgucode=11) + 장르 + 31일 단위 기간, 페이지 반복
   │  mt20id
   ▼
공연상세 pblprfr/{mt20id}  시설ID(mt10id), 예매처(relates), 가격·시간
   │  mt10id
   ▼
시설상세 prfplc/{mt10id}   좌표(la, lo), 주소  ← 처음 보는 시설만 호출, Venue 테이블에 저장
```

1. 장르는 `CCCD`(대중음악), `GGGA`(뮤지컬)부터 시작
2. `mt20id` 기준으로 있으면 갱신, 없으면 추가 (중복 방지)
3. 화면 하단에 KOPIS 출처 표기

---

## 6. 나중에 쓸 수 있는 API (추가 기능 후보)

| API | URL | 쓸 수 있는 기능 |
|---|---|---|
| **예매상황판** | `boxoffice` | "지금 인기 공연" 순위. 요청: `stdate`, `eddate`, `catecode`(장르), `area`(`11`=서울), `srchseatscale`(좌석 규모). 응답에 `rnum`(순위), `mt20id`가 있어 우리 공연 데이터와 바로 연결 가능 |
| 축제 목록 | `prffest` | 축제 공연만 따로 보기 (공연목록과 같은 요청 변수, 응답에 `festival`) |
| 수상작 목록 | `prfawad` | 수상 공연 뱃지 (응답에 `awards`) |

나머지(기획/제작사, 원·창작자, 각종 예매·공연 통계)는 이 서비스와 관련이 적어 생략.
통계 API는 대부분 `stdate`/`eddate`(최대 31일)만 받고 페이지가 없음.
