package org.ecommerce.auth.service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ecommerce.auth.service.entity.UserCredentialEntity;
import org.ecommerce.auth.service.exception.DownstreamServiceException;
import org.ecommerce.auth.service.integration.adapter.UserAdapter;
import org.ecommerce.auth.service.integration.dto.UserDto;
import org.ecommerce.auth.service.repositories.UserCredentialRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Profile("seed")
@Log4j2
public class DatabaseSeederConfig implements CommandLineRunner {

    private static final int MAX_ATTEMPTS = 6;
    private static final long INITIAL_BACKOFF_MS = 2_000; // 2 seconds

    // This class is responsible for seeding the database with initial data.
    private final UserCredentialRepository credentialRepository;
    private final UserAdapter userAdapter;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.enabled:false}")
    private boolean seedEnabled;
    @Value("${admin.seeder.email}")
    private String admin;
    @Value("${admin.seeder.password}")
    private String password;

    @Override
    @NullMarked
    public void run(String... args) {

        if (!seedEnabled) {
            log.info("Admin credential seeding disabled (admin.seed.enabled=false), skipping");
            return;
        }
        if (!StringUtils.hasText(password)) {
            log.warn("admin.seed.password is not set - skipping admin credential seeding. " +
                    "Set ADMIN_SEED_PASSWORD (env var / SSM) to enable it.");
            return;
        }
        UserDto userDto = fetchUserWithRetry(admin);
        if (userDto == null) {
            log.error("Failed to fetch user by email after {} attempts, skipping seeding", MAX_ATTEMPTS);
            return;
        }

        if (credentialRepository.findByUserAccountIdAndActiveIsTrue(userDto.getUserAccountId()).isPresent()) {
            log.info("Admin credentials already exist for email: {}, skipping seeding", admin);
            return;
        }

        UserCredentialEntity userCredentialEntity = new UserCredentialEntity();
        userCredentialEntity.setUserAccountId(userDto.getUserAccountId());
        userCredentialEntity.setPassword(passwordEncoder.encode(password));
        userCredentialEntity.setCreatedBy("system");
        userCredentialEntity.setUpdatedBy("system");
        credentialRepository.save(userCredentialEntity);
        log.info("Admin credentials seeded successfully for email: {}", admin);

    }

    private UserDto fetchUserWithRetry(String email) {
        long backoffMs = INITIAL_BACKOFF_MS;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return userAdapter.getUserByEmail(email);
            } catch (DownstreamServiceException e) {
                log.warn("Admin lookup attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, e.getMessage());
                sleep(backoffMs);
                backoffMs *= 2; // Exponential backoff
            }

        }
        return null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.error("Sleep interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
}