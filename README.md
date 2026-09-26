# 혜자 백엔드

## 테스트

```bash
./gradlew test       # 단위·Controller·Service·Repository 테스트
./gradlew e2eTest    # Testcontainers 기반 핵심 API E2E 테스트
```

E2E 테스트는 Docker에서 MariaDB와 Redis를 자동으로 실행하므로 로컬 DB나 Redis를 따로 켤 필요가 없습니다.
검증하는 사용자 여정과 실행 환경은 [`docs/e2e-testing.md`](docs/e2e-testing.md)를 참고합니다.
