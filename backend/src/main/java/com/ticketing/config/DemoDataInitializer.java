package com.ticketing.config;

import com.ticketing.auth.mapper.AccountMapper;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoDataInitializer implements ApplicationRunner {
    private final AccountMapper mapper;
    private final PasswordEncoder encoder;
    private final String password;

    public DemoDataInitializer(AccountMapper mapper, PasswordEncoder encoder,
                               @Value("${app.demo.password}") String password) {
        this.mapper = mapper;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("DEMO_PASSWORD must be 12+ characters and at most 72 UTF-8 bytes");
        }
        create("requester1", "小李", "REQUESTER");
        create("requester2", "小周", "REQUESTER");
        create("coordinator1", "协调员", "COORDINATOR");
        create("support1", "技术支持一", "SUPPORT");
        create("support2", "技术支持二", "SUPPORT");
    }

    private void create(String username, String displayName, String role) {
        if (mapper.findByUsername(username) != null) return;
        mapper.insertDemoUser(username, displayName, encoder.encode(password));
        var account = mapper.findByUsername(username);
        if (mapper.assignRole(account.id(), role) != 1) {
            throw new IllegalStateException("Demo role is not initialized");
        }
    }
}
