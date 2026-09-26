package com.hyeja.e2e.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hyeja.domain.member.repository.MemberRepository;
import com.hyeja.domain.notification.repository.NotificationRepository;
import com.hyeja.domain.policy.entity.Policy;
import com.hyeja.domain.policy.enums.PolicyCategory;
import com.hyeja.domain.policy.repository.PolicyRepository;
import com.hyeja.domain.profile.repository.ProfileRepository;
import com.hyeja.domain.region.entity.Region;
import com.hyeja.domain.region.repository.RegionRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "e2e"})
@Import(E2eContainerConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiE2eTestSupport {

    protected static final String REGION_CODE = "11440";
    protected static final String REGION_NAME = "서울특별시 마포구";
    protected static final String PASSWORD = "hyeja1234!";
    protected static final LocalDate FIXED_TODAY = LocalDate.of(2026, 9, 27);

    private static final List<String> TABLES = List.of(
            "notification",
            "favorite",
            "policy_region",
            "profile",
            "member",
            "policy",
            "region",
            "card_news",
            "term"
    );

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    protected RegionRepository regionRepository;

    @Autowired
    protected MemberRepository memberRepository;

    @Autowired
    protected ProfileRepository profileRepository;

    @Autowired
    protected PolicyRepository policyRepository;

    @Autowired
    protected NotificationRepository notificationRepository;

    @BeforeEach
    void resetState() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        try {
            TABLES.forEach(table -> jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`"));
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    protected Region saveDefaultRegion() {
        return regionRepository.saveAndFlush(Region.builder()
                .regionCode(REGION_CODE)
                .sigunguName(REGION_NAME)
                .build());
    }

    protected Policy savePolicy(
            String policyId,
            String policyName,
            PolicyCategory category,
            LocalDate applyEndDate,
            int viewCount,
            boolean active
    ) {
        return policyRepository.saveAndFlush(Policy.builder()
                .policyId(policyId)
                .policyName(policyName)
                .category(category)
                .description(policyName + " 설명")
                .supportContent(policyName + " 지원 내용")
                .ageLimitYn(false)
                .applyPeriodCode(applyEndDate == null ? "ALWAYS" : "LIMITED")
                .applyStartDate(FIXED_TODAY.minusDays(30))
                .applyEndDate(applyEndDate)
                .applyUrl("https://example.com/" + policyId)
                .viewCount(viewCount)
                .activeYn(active)
                .build());
    }

    protected Session signupAndLogin() throws Exception {
        return signupAndLogin("hyeja@example.com", "혜자회원");
    }

    protected Session signupAndLogin(String email, String nickname) throws Exception {
        saveDefaultRegion();

        ApiHttpResponse signup = request("POST", "/api/members", signupBody(email, nickname), null);
        if (signup.status() != 200) {
            throw new IllegalStateException("회원가입 실패: " + signup.body());
        }

        ApiHttpResponse login = request(
                "POST",
                "/api/members/login",
                """
                        {"email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD),
                null
        );
        if (login.status() != 200) {
            throw new IllegalStateException("로그인 실패: " + login.body());
        }

        return new Session(
                login.body().path("result").path("memberId").asLong(),
                login.body().path("result").path("accessToken").asText(),
                email,
                nickname
        );
    }

    protected String signupBody(String email, String nickname) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "nickname": "%s",
                  "profile": {
                    "birth": "2000-03-15",
                    "regionCode": "%s",
                    "employmentCode": "EMPLOYED",
                    "houselessYn": true,
                    "marriageCode": "SINGLE",
                    "incomeRangeCode": "R2000_3000",
                    "educationCode": "COLLEGE_GRADUATE",
                    "housingType": "MONTHLY_RENT"
                  }
                }
                """.formatted(email, PASSWORD, nickname, REGION_CODE);
    }

    protected ApiHttpResponse request(String method, String path, String body, String accessToken)
            throws Exception {
        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json")
                .method(method, publisher);
        if (body != null) {
            builder.header("Content-Type", "application/json");
        }
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        HttpResponse<String> response = httpClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        );
        return new ApiHttpResponse(response.statusCode(), objectMapper.readTree(response.body()));
    }

    protected record Session(Long memberId, String accessToken, String email, String nickname) {
    }

    protected record ApiHttpResponse(int status, JsonNode body) {
    }
}
