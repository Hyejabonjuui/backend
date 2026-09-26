# API 규칙과 명세 상태

## 기준

- 비즈니스 API prefix는 `/api`다.
- 실제 계약을 확인할 때는 Controller, DTO, 서비스 테스트를 먼저 본다.
- Figma는 사용자 흐름과 필드 요구사항을 확인하는 두 번째 기준이다.
- Notion API 명세는 계획과 협업용 기준이며, 구현과 다르면 차이를 기록한 뒤 명세를 갱신한다.
- 배포 서버 URL은 없으며 로컬 `http://localhost:8080`을 기본으로 본다.

## 공통 응답

모든 API 성공 응답은 HTTP `200 OK`, `SUCCESS_001`과 함께 다음 형태의 `ApiResponse<T>`를 사용한다.

```json
{
  "isSuccess": true,
  "code": "SUCCESS_001",
  "message": "성공입니다.",
  "result": {}
}
```

상태 코드는 `global.apiPayload.code.status`의 실제 enum을 사용한다. Notion 예시에 남아 있는 `COMMON200` 같은 코드를 그대로 복사하지 않는다.

## 실제 구현된 엔드포인트

2026-09-27 현재 Controller 기준이다.

| Method | Path | 입력 | 로그인 | 비고 |
| --- | --- | --- | --- | --- |
| `GET` | `/api/health` | 없음 | 불필요 | 공통 래퍼의 `result.status`로 `UP` 반환 |
| `POST` | `/api/members` | body 계정 + `profile` | 불필요 | 회원가입(계정과 내 조건을 한 번에) |
| `POST` | `/api/members/login` | body `email`, `password` | 불필요 | accessToken(30분)·memberId·nickname 반환 |
| `POST` | `/api/members/logout` | 없음 | 필요 | 토큰을 Redis 블랙리스트에 등록 |
| `GET` | `/api/members/find-email` | query `nickname`, `birth` | 불필요 | 가린 이메일과 가입일 |
| `GET` | `/api/members/me` | 없음 | 필요 | 내 계정 조회 |
| `PATCH` | `/api/members/me/delete` | 없음 | 필요 | 회원 탈퇴(soft delete) |
| `GET` | `/api/members/me/profile` | 없음 | 필요 | 내 조건 조회 |
| `PATCH` | `/api/members/me/profile` | body 내 조건 | 필요 | 내 조건 전체 교체 |
| `GET` | `/api/regions` | 없음 | 불필요 | 시·도별 시군구 목록 |
| `GET` | `/api/policies/card-news/guest` | 없음 | 불필요 | 대표 카드 중 최대 4건 |
| `GET` | `/api/policies/housing` | query `category?`, `sort=DEADLINE`, `page=0`, `size=8` | 불필요 | 진행 중 정책 페이지 조회, 상시 정책은 마감일순 마지막 배치 |
| `GET` | `/api/policies/{policyId}` | path `policyId` | 필요 | 회원 맞춤 정보를 포함한 정책 상세 |
| `POST` | `/api/policies/sync` | 없음 | 필요 | 외부 정책 수동 동기화 |
| `GET` | `/api/favorite` | query `keyword?`, `page=0`, `size=8` | 필요 | 관심 정책을 최근 등록순으로 페이지 조회, 정책명·지원 내용 검색 |
| `POST` | `/api/favorite/{policyId}` | path `policyId` | 필요 | 관심 정책 등록, 중복 등록 불가 |
| `DELETE` | `/api/favorite/{policyId}` | path `policyId` | 필요 | 관심 정책 영구 삭제 |
| `GET` | `/api/notification` | query `page=0`, `size=8` | 필요 | 삭제되지 않은 알림 최신순 페이지 조회 |
| `PATCH` | `/api/notification/{notificationId}/read` | path `notificationId` | 필요 | 본인 소유의 알림 읽음 처리 |
| `DELETE` | `/api/notification/{notificationId}` | path `notificationId` | 필요 | 본인 소유의 알림 영구 삭제 |
| `POST` | `/api/notification/admin/generate` | query `memberId: Long` | 필요 | 개발·테스트용, 지정 회원의 D-7 관심 정책 알림만 생성 |

Swagger UI는 `/swagger-ui.html`, OpenAPI JSON은 `/v3/api-docs`에서 확인한다.

## 명세에 있으나 구현 전인 주요 API

아래 목록은 Notion 명세의 계획을 요약한 것이며 현재 코드의 존재를 뜻하지 않는다.

- 정책: 검색·추천·카드 상세
- 회원용 카드뉴스

구현 전에 Method와 URL을 다시 확인한다. 명세에는 `/api` 누락, memberId 위치 불일치, `notificatonId` 오타 등 현재 코드와 다른 표기가 남아 있다.

## 인증 규칙

- 로그인(`POST /api/members/login`)이 JWT accessToken(30분)을 발급하고, 로그아웃(`POST /api/members/logout`)이 토큰을 Redis 블랙리스트에 올린다.
- 로그인이 필요한 API는 요청 헤더에 `Authorization: Bearer <accessToken>`을 보낸다. 토큰이 없거나 잘못됐거나 로그아웃한 토큰이면 `401 COMMON_002`다.
- 회원은 `memberId` 파라미터가 아니라 토큰으로 식별한다. 컨트롤러는 `@AuthenticationPrincipal Long memberId`로 받는다.
- 기본은 로그인 필수이고, 로그인 없이 쓰는 API만 `SecurityConfig`에 허용 목록으로 둔다. 비로그인용 API를 새로 만들면 허용 목록에 추가한다.
- 예외: 개발·테스트용 `POST /api/notification/admin/generate`의 `memberId`는 "알림을 만들 대상 회원"이라 query로 유지한다.

## 필드와 직렬화

- Java DTO의 nullable 여부와 validation을 계약의 출발점으로 삼는다.
- JSON 필드명은 기존 DTO의 Jackson 설정을 확인한다. snake_case와 camelCase를 임의로 일괄 변경하지 않는다.
- 외부 정책 ID는 숫자로 변환하지 않고 문자열로 유지한다.
- CardNews `title`은 배열이 아니라 nullable 문자열이다.
- 알림 응답의 `apply_end_date`는 알림 생성 시 저장한 정책 마감일 스냅샷이다.
- enum은 표시 문구가 아니라 코드의 enum 상수와 converter 규칙을 사용한다.

## API 추가·수정 체크리스트

1. 실제 Controller의 Method, `/api` 포함 경로, path/query/body를 정한다.
2. DTO의 타입, 필수값, nullable, 길이, enum을 명시한다.
3. 성공과 실패 상태 코드를 기존 `ApiResponse` 체계에 연결한다.
4. 인증이 필요한지와 현재 회원 식별 방식을 명확히 한다.
5. Controller·Service·Repository 테스트를 위험도에 맞게 추가한다.
6. Swagger에서 실제 스키마를 확인한다.
7. Notion 명세와 프론트엔드 계약을 실제 구현에 맞춰 갱신한다.
