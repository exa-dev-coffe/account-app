package com.account_service.be;

import com.account_service.be.account.AccountModel;
import com.account_service.be.account.AccountRepository;
import com.account_service.be.lib.JwtService;
import com.account_service.be.lib.RabbitmqService;
import com.account_service.be.role.RoleModel;
import com.account_service.be.utils.HmacUtils;
import com.account_service.be.utils.PasswordUtils;
import com.account_service.be.utils.enums.TokenType;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class BaseIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");
    protected static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);
    protected static final GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:RELEASE.2024-01-16T16-07-38Z"))
            .withEnv("MINIO_ROOT_USER", "minioadmin")
            .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
            .withCommand("server", "/data")
            .withExposedPorts(9000);

    static {
        postgres.start();
        redis.start();
        minio.start();

        try {
            MinioClient client = MinioClient.builder()
                    .endpoint("http://" + minio.getHost() + ":" + minio.getFirstMappedPort())
                    .credentials("minioadmin", "minioadmin")
                    .build();
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket("coffe").build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket("coffe").build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);

        registry.add("minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getFirstMappedPort());
        registry.add("minio.url", () -> "http://" + minio.getHost() + ":" + minio.getFirstMappedPort());
        registry.add("minio.accessKey", () -> "minioadmin");
        registry.add("minio.secretKey", () -> "minioadmin");
        registry.add("minio.bucketName", () -> "coffe");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected AccountRepository accountRepository;

    @Autowired
    protected JwtService jwtService;

    @Autowired
    protected HmacUtils hmacUtils;

    @Autowired
    protected RedisTemplate<String, Object> redisTemplate;

    @Autowired
    protected org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Autowired
    protected com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @MockBean
    protected RabbitmqService rabbitmqService;

    protected AccountModel createTestAccount(String email, String roleName) {
        jdbcTemplate.execute("INSERT INTO tm_roles (role_id, role_name) VALUES (1, 'admin'), (2, 'user'), (3, 'barista') ON CONFLICT (role_id) DO NOTHING");
        jdbcTemplate.execute("INSERT INTO tm_features (feature_id, feature_key, feature_name) VALUES (1, 'catalog', 'Manage Catalog'), (2, 'category', 'Manage Categories'), (3, 'table', 'Manage Tables'), (4, 'voucher', 'Manage Vouchers'), (5, 'promotion', 'Manage Promotions'), (6, 'barista', 'Manage Baristas'), (7, 'order', 'Incoming Orders'), (8, 'inventory', 'Inventory Status'), (9, 'report', 'Analytics Overview'), (10, 'role_management', 'Role Management'), (11, 'user_management', 'Manage Users'), (12, 'pos', 'Point of Sale (POS)') ON CONFLICT (feature_id) DO NOTHING");
        jdbcTemplate.execute("INSERT INTO tm_role_features (role_id, feature_id, can_view, can_create, can_edit, can_delete) VALUES (1, 1, true, true, true, true), (1, 2, true, true, true, true), (1, 3, true, true, true, true), (1, 4, true, true, true, true), (1, 5, true, true, true, true), (1, 6, true, true, true, true), (1, 7, true, true, true, true), (1, 8, true, true, true, true), (1, 9, true, true, true, true), (1, 10, true, true, true, true), (1, 11, true, true, true, true), (1, 12, true, true, true, true), (3, 6, true, true, true, true), (3, 12, true, true, true, true) ON CONFLICT (role_id, feature_id) DO NOTHING");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('tm_roles', 'role_id'), COALESCE((SELECT MAX(role_id) FROM tm_roles), 1))");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('tm_features', 'feature_id'), COALESCE((SELECT MAX(feature_id) FROM tm_features), 1))");
        jdbcTemplate.execute("SELECT setval(pg_get_serial_sequence('tm_role_features', 'id'), COALESCE((SELECT MAX(id) FROM tm_role_features), 1))");

        AccountModel existing = accountRepository.findByEmail(email);
        if (existing != null) {
            return existing;
        }

        RoleModel role = new RoleModel();
        if ("admin".equalsIgnoreCase(roleName)) {
            role.setRoleId(1);
            role.setRoleName("admin");
        } else if ("barista".equalsIgnoreCase(roleName)) {
            role.setRoleId(3);
            role.setRoleName("barista");
        } else {
            role.setRoleId(2);
            role.setRoleName("user");
        }

        AccountModel account = new AccountModel();
        account.setEmail(email);
        account.setPassword(PasswordUtils.hashPassword("Password123!"));
        account.setFullName("Test " + roleName);
        account.setPhoto("http://example.com/avatar.jpg");
        account.setRole(role);
        return accountRepository.save(account);
    }

    protected String createTestJwt(AccountModel account) {
        return jwtService.createToken(account, TokenType.ACCESS);
    }

    protected String createHmacSignature(String query, String timestamp, String body) throws Exception {
        String message = (query == null ? "" : query) + timestamp + (body == null ? "" : body);
        return hmacUtils.generateHMAC(message);
    }
}
