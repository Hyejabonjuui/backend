# 혜자 백엔드 작업 가이드

이 저장소는 청년 주거 정책 서비스 **혜자**의 Spring Boot 백엔드다. 작업을 시작할 때 이 파일만으로 모든 문서를 한꺼번에 읽지 말고, 먼저 실제 코드와 변경 대상의 주변 테스트를 확인한 뒤 아래 문서 중 필요한 것만 읽는다.

## 기준과 범위

- 현재 동작을 판단할 때는 **실제 코드 > Figma > API 명세·ERD·기타 문서** 순으로 본다.
- 현재 작업의 명시적 요구사항은 목표 상태를 정한다. 최근에 확정한 결정은 오래된 계획서보다 우선한다.
- 목표와 현재 코드가 다르면 해당 작업 범위에서만 고치고, 범위 밖의 차이는 임의로 섞지 말고 결과에 명시한다.
- 현재 범위는 웹 전용 반응형 서비스다.
- 지도, 캘린더, 관리자 기능은 현재 범위에서 제외한다.
- AI 추천·요약과 토큰 기반 인증은 추후 개발 항목이다.
- 배포 환경은 운영하지 않으며 로컬 개발과 테스트를 기준으로 한다.

## 필요한 문서만 읽기

- 제품 목적·현재 범위: `docs/project-overview.md`
- 기술 스택·패키지·실행 흐름: `docs/architecture.md`
- 엔티티·관계·ERD 정합성: `docs/domain-model.md`
- 엔드포인트·응답·명세 관리: `docs/api-conventions.md`
- 이슈·브랜치·커밋·PR 규칙: `docs/development-workflow.md`
- Figma·Notion·저장소 링크: `docs/references.md`
- 로컬 DB와 개발 시드: `docs/dev-seed.md`
- Profile·Policy enum: `docs/profile-policy-enums.md`

## 구현 원칙

- `com.hyeja.domain.<domain>` 아래에서 controller, service, repository, entity, dto, converter, enums 계층을 기존 도메인 구조에 맞춰 사용한다.
- 성공·실패 응답은 `global.apiPayload`의 `ApiResponse`, `BaseCode`, `ErrorStatus` 체계를 우선 사용한다.
- 엔티티는 `BaseEntity`를 상속하지만, 소프트 삭제와 조회 제외는 자동이 아니다. 각 기능의 기존 삭제·조회 규칙을 확인한다.
- DTO 변환 방식, JSON 필드명, nullable 여부는 인접 코드와 API 계약을 함께 확인한다.
- 새 제약이나 관계 변경에는 엔티티 테스트와 필요한 서비스·컨트롤러 테스트를 추가한다.
- 현재 없는 인증을 있다고 가정하지 않는다. 개발 단계의 회원 식별자는 기존 API처럼 path/query의 `memberId`를 사용할 수 있다.
- 환경변수 값과 비밀키는 커밋하지 않는다. `.env` 파일을 읽거나 출력에 노출하지 않는다.

## 검증 명령

```bash
./gradlew test
./gradlew test --tests "패키지.테스트클래스명"
./gradlew bootRun
```

- 테스트는 H2를 사용하고, 로컬 애플리케이션은 MariaDB를 사용한다.
- PR CI는 `./gradlew test --no-daemon`을 실행한다.
- 문서만 변경해도 `git diff --check`와 링크·경로 확인을 수행한다.

## 작업 안전 수칙

- 작업 전에 `git status --short`와 현재 브랜치를 확인한다.
- 사용자의 기존 변경과 무관한 미추적 파일을 수정·삭제·커밋하지 않는다.
- `build/`, `bin/`, `tmp/`, IDE 설정, 비밀값을 작업 산출물에 포함하지 않는다.
- 관련 없는 리팩터링이나 포맷 변경을 섞지 않는다.
- 아래 확정 사항은 별도 구현 작업 전까지 현재 코드와 다를 수 있으므로, 무관한 이슈에서 몰래 함께 수정하지 않는다.
  - `Profile.member`를 `@ManyToOne`에서 `@OneToOne`으로 변경
  - Member/Profile 이메일 길이를 DB·ERD까지 100자로 통일
  - 외부 정책 ID와 모든 관련 FK를 100자로 통일

## Git 협업

- 한 이슈에는 한 브랜치를 사용한다.
- 브랜치 기본 형식은 `<tag>/<issue-number>/<english-description>`이다. 이슈에서 브랜치명을 명시하면 그 이름을 우선한다.
- 커밋 기본 형식은 `<tag> : <변경 내용>`이다.
- PR 제목은 이슈 제목과 맞추고 본문에 `Closes #<issue-number>`를 적는다.
- 커밋·푸시·PR 생성은 사용자가 요청한 범위에서만 수행한다.
