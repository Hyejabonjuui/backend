# 프로젝트 참고 자료

외부 링크는 접근 권한이나 로그인 상태에 따라 열리지 않을 수 있다. 링크의 내용보다 현재 저장소 코드와 최근 팀 결정을 우선한다.

## 디자인과 명세

- [Figma — 혜자](https://www.figma.com/design/19OJDjkZqMnGthjP4aCi0N/%ED%98%9C%EC%9E%90?node-id=453-1738&t=UJHDN15i4J9eSnpJ-1)
- [Notion — API 명세서](https://app.notion.com/p/API-3e382c18756e80cd9119ca6d6fb50ce1?source=copy_link)
- [Notion — Git/GitHub 행동 지침](https://shared-wool-66d.notion.site/Git-GitHub-3e382c18756e80d1b176cbdfe9e63a98)

Figma에서 확인되는 주요 화면 묶음은 회원가입·조건 등록, 로그인, 홈·헤더, 검색·추천 결과, 카드뉴스·정책 상세, 마이페이지, 알림함, 관심 정책이다. 관리자 화면에는 추후 개발 표시가 있다.

## 저장소

- [백엔드](https://github.com/Hyejabonjuui/backend)
- [프론트엔드](https://github.com/Hyejabonjuui/frontend)

현재 저장소는 백엔드다.

## 저장소 내부 문서

- `AGENTS.md`: 새 작업의 진입점과 문서 라우팅
- `docs/project-overview.md`: 현재 제품 범위와 우선순위
- `docs/architecture.md`: 기술 구조와 실행 설정
- `docs/domain-model.md`: 엔티티, 관계, 확정된 정합성 작업
- `docs/api-conventions.md`: 현재 API와 명세 규칙
- `docs/development-workflow.md`: 이슈부터 PR까지의 협업 규칙
- `docs/dev-seed.md`: 로컬 DB와 개발 시드
- `docs/profile-policy-enums.md`: Profile·Policy enum 저장 규칙

## 역사적 자료 취급

수행계획서, 수강생 안내서, ERD 이미지와 SQL 내보내기는 프로젝트 배경을 이해하는 자료다. 저장소에 버전 관리되지 않은 로컬 파일 경로에 의존하지 않는다.

- 초기 수행계획서의 지도·캘린더·AI 범위는 현재 결정으로 대체되었다.
- 기존 SQL 내보내기에는 email FK·길이, CardNews PK, 누락 FK, BASE_ENTITY 테이블 등 오래된 내용이 있다.
- API 명세에는 아직 구현되지 않은 경로와 오탈자, 인증 도입을 전제로 한 설명이 섞여 있다.
- ERD는 확정 방향을 보여주지만 실제 구현 여부는 엔티티와 테스트에서 다시 확인한다.
