package wemove.identity.config;

import wemove.config.WemoveProperties;
import wemove.identity.domain.*;
import wemove.identity.repository.UserRepository;
import java.time.Instant;
import java.util.Locale;
import org.slf4j.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final WemoveProperties properties;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrap(WemoveProperties properties, UserRepository users, PasswordEncoder encoder) {
        this.properties = properties;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = properties.bootstrap().adminEmail();
        String password = properties.bootstrap().adminPassword();
        if (email == null || email.isBlank()) {
            log.warn("BOOTSTRAP_ADMIN_EMAIL 未设置，未初始化管理员。");
            return;
        }
        if (password == null || password.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD 必须至少 12 个字符");
        }
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (users.existsByEmailNormalized(normalized)) return;
        Instant now = Instant.now();
        users.save(UserEntity.create(email.strip(), normalized, encoder.encode(password),
            properties.bootstrap().adminNickname().strip(), BaseRole.ADMIN, now));
        log.info("已初始化管理员账户 email={}", normalized);
    }
}
