# 개발 워크플로

팀의 Git/GitHub 행동 지침과 저장소 템플릿을 요약한 문서다. 외부 지침과 현재 이슈의 명시적 요청이 다르면 현재 이슈 요청을 우선한다.

## 기본 흐름

```text
이슈 생성 → 이슈 전용 브랜치 → 작은 커밋 → 테스트 → 최신 main 반영 → push → PR → 리뷰·CI → merge
```

한 브랜치에 여러 이슈를 섞지 않는다.

## 이슈

- 제목 기본 형식: `<tag>: <요약>`
- 저장소의 `.github/ISSUE_TEMPLATE/작업-이슈.md`를 사용한다.
- 작업 목적, 작업 내용, 완료 조건, 참고 사항을 구체적으로 적는다.
- assignee와 label은 이슈 성격에 맞춰 지정한다.

주요 tag는 `feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `infra` 등이다. 기존 팀 문서의 오탈자나 예전 tag보다 해당 저장소에서 실제 사용하는 label과 이슈 지시를 우선한다.

## 브랜치

기본 형식:

```text
<tag>/<issue-number>/<english-description>
```

명시된 브랜치명이 없을 때의 형식 예시:

```text
fix/42/profile-member-relation
chore/32/agent-context-setting
```

사용자가 정확한 브랜치명을 지정하면 형식이 조금 다르더라도 그 이름을 사용한다. 브랜치를 만들기 전 같은 이름의 로컬·원격 브랜치가 있는지 확인한다.

## 커밋

기본 형식:

```text
<tag> : <변경 내용>
```

예시:

```text
docs : 에이전트 컨텍스트 문서 추가
fix : 프로필 회원 관계를 일대일로 변경
```

- 하나의 논리적 변경 단위로 커밋한다.
- 관련 없는 파일과 사용자의 기존 변경을 포함하지 않는다.
- 자동 생성물, 비밀값, `.env`, `build/`, `tmp/`를 커밋하지 않는다.

## 검증

변경 위험도에 따라 최소한 다음을 수행한다.

```bash
./gradlew test
git diff --check
git status --short
```

특정 영역만 빠르게 확인할 때는 다음 형식을 사용할 수 있다.

```bash
./gradlew test --tests "com.hyeja.domain.member.*"
```

CI는 PR과 수동 실행에서 `./gradlew test --no-daemon`을 수행한다.
환경변수는 저장소 Settings → Secrets and variables → Actions에 등록한 Secrets(`YOUTH_API_KEY`, `OPEN_AI_KEY`, `OPEN_AI_MODEL`, `JWT_SECRET`)에서 읽는다(`.github/workflows/ci.yml`).
`JWT_SECRET`은 `application-test.yml`의 테스트용 값보다 우선하므로, 32자보다 짧거나 비어 있으면 CI 테스트가 실패한다.

## PR

- `.github/pull_request_template.md`를 사용한다.
- PR 제목은 이슈 제목과 일치시키는 것을 기본으로 한다.
- 본문에 `Closes #<issue-number>`를 넣는다.
- 변경 사항, 테스트 방법과 결과, 리뷰 시 주의점을 적는다.
- 최신 main과의 충돌을 확인하고 CI 통과 후 리뷰를 받는다.
- 외부 메시지 전송, reviewer 호출, merge는 사용자가 요청한 범위에서만 수행한다.

## 문서 정합성

기능이나 계약이 바뀌면 코드만 고치고 끝내지 않는다.

- API 변경: Swagger와 Notion 명세
- 엔티티·제약 변경: 테스트, ERD, SQL 또는 마이그레이션
- enum 변경: `docs/profile-policy-enums.md`
- 환경변수·시드 변경: `docs/dev-seed.md`와 실행 문서
- 제품 범위 변경: `docs/project-overview.md`
