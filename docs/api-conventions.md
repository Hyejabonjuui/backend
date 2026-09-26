# API 규칙과 명세 상태

## 기준

- 비즈니스 API prefix는 `/api`다.
- 실제 계약을 확인할 때는 Controller, DTO, 서비스 테스트를 먼저 본다.
- Figma는 사용자 흐름과 필드 요구사항을 확인하는 두 번째 기준이다.
- Notion API 명세는 계획과 협업용 기준이며, 구현과 다르면 차이를 기록한 뒤 명세를 갱신한다.
- 배포 서버 URL은 없으며 로컬 `http://localhost:8080`을 기본으로 본다.

## 공통 응답

헬스체크를 제외한 현재 API는 다음 형태의 `ApiResponse<T>`를 사용한다.

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

2026-09-25 현재 Controller 기준이다.

| Method | Path | 입력 | 비고 |
| --- | --- | --- | --- |
| `GET` | `/api/health` | 없음 | `{ "status": "UP" }`, 공통 래퍼 미사용 |
| `GET` | `/api/policies/card-news/guest` | 없음 | 대표 카드 중 최대 4건 |
| `GET` | `/api/members/me` | query `memberId: Long` | JWT 전 임시 회원 식별 방식 |
| `GET` | `/api/favorite` | query `memberId: Long`, `page=0`, `size=8` | 회원의 관심 정책을 최근 등록순으로 페이지 조회 |
| `POST` | `/api/favorite/{policyId}` | path `policyId: String`, query `memberId: Long` | 관심 정책 등록, 중복 등록 불가 |
| `GET` | `/api/notification/{memberId}` | path `memberId`, query `page=0`, `size=8` | 삭제되지 않은 알림 최신순 페이지 조회 |
| `DELETE` | `/api/notification/{notificationId}` | path `notificationId`, query `memberId` | 본인 소유의 삭제되지 않은 알림 영구 삭제 |
| `PATCH` | `/api/notification/{notificationId}/read` | path `notificationId`, query `memberId` | 본인 소유의 삭제되지 않은 알림 읽음 처리 |
| `POST` | `/api/policies/sync` | 없음 | 외부 정책 수동 동기화 |
| `GET` | `/api/policies/housing` | 없음 | 현재는 저장된 Policy 전체 조회 |

Swagger UI는 `/swagger-ui.html`, OpenAPI JSON은 `/v3/api-docs`에서 확인한다.

## 명세에 있으나 구현 전인 주요 API

아래 목록은 Notion 명세의 계획을 요약한 것이며 현재 코드의 존재를 뜻하지 않는다.

- 인증·회원: 로그인, 로그아웃, 회원가입, 이메일 찾기, 회원 탈퇴
- 프로필: 생성, 조회, 수정
- 지역 목록
- 정책: 검색·추천·상세·페이지 목록·카드 상세
- 회원용 카드뉴스
- 관심 정책 해제

구현 전에 Method와 URL을 다시 확인한다. 명세에는 `/api` 누락, memberId 위치 불일치, `notificatonId` 오타 등 현재 코드와 다른 표기가 남아 있다.

## 인증 전환기 규칙

- 토큰 기반 인증은 아직 구현하지 않는다.
- 현재 개발 단계에서는 기존 코드처럼 `memberId`를 path variable이나 query string으로 받을 수 있다.
- 최종적으로 헤더에서 인증 사용자를 구하는 방향이지만, 현재 없는 Authorization 처리나 Redis 토큰 블랙리스트를 가정해서 구현하지 않는다.
- 인증을 도입할 때 회원 식별 파라미터 제거 여부와 모든 회원 API 계약을 함께 갱신한다.

## 필드와 직렬화

- Java DTO의 nullable 여부와 validation을 계약의 출발점으로 삼는다.
- JSON 필드명은 기존 DTO의 Jackson 설정을 확인한다. snake_case와 camelCase를 임의로 일괄 변경하지 않는다.
- 외부 정책 ID는 숫자로 변환하지 않고 문자열로 유지한다.
- CardNews `title`은 배열이 아니라 nullable 문자열이다.
- enum은 표시 문구가 아니라 코드의 enum 상수와 converter 규칙을 사용한다.

## API 추가·수정 체크리스트

1. 실제 Controller의 Method, `/api` 포함 경로, path/query/body를 정한다.
2. DTO의 타입, 필수값, nullable, 길이, enum을 명시한다.
3. 성공과 실패 상태 코드를 기존 `ApiResponse` 체계에 연결한다.
4. 인증이 필요한지와 현재 회원 식별 방식을 명확히 한다.
5. Controller·Service·Repository 테스트를 위험도에 맞게 추가한다.
6. Swagger에서 실제 스키마를 확인한다.
7. Notion 명세와 프론트엔드 계약을 실제 구현에 맞춰 갱신한다.
