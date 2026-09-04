package org.ecommerce.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.ecommerce.user.entity.RoleEntity;
import org.ecommerce.user.entity.UserEntity;
import org.ecommerce.user.entity.UserRoleEntity;
import org.ecommerce.user.repositories.RoleRepository;
import org.ecommerce.user.repositories.UserRepository;
import org.ecommerce.user.utils.Gender;
import org.ecommerce.user.utils.RoleConstants;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Log4j2
@Profile("seed")
public class DatabaseSederConfig implements CommandLineRunner {

    // -----------DEPENDENCIES----------------
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminConfig adminConfig;

    @Override
    @NullMarked
    public void run(String... args) {

        if (!adminConfig.isEnabled()) {
            log.info("Admin config is disabled (admin.database.seeder.enabled=false), skipping");
            return;
        }

        if (userRepository.findByEmail(adminConfig.getEmail()).isPresent()) {
            log.info("Admin user already exists {email={}}, skipping seeding", adminConfig.getEmail());
            return;
        }

        // fetch the admin role from the database
        RoleEntity roleEntity = roleRepository.findByName(RoleConstants.ADMIN).orElse(null);
        if (ObjectUtils.isEmpty(roleEntity)) {
            log.error("Admin role not found in the database, skipping seeding");
            return;
        }
        // create a new user entity and set the properties from the adminConfig
        UserEntity admin = new UserEntity();
        admin.setFirstName(adminConfig.getFirstName());
        admin.setLastName(adminConfig.getLastName());
        admin.setEmail(adminConfig.getEmail());
        admin.setDateOfBirth(LocalDate.parse(adminConfig.getDateOfBirth()));
        admin.setGender(Gender.valueOf(adminConfig.getGender().toUpperCase()));
        admin.setMobile(adminConfig.getMobile());
        admin.setCreatedBy("system");
        admin.setUpdatedBy("system");

        UserRoleEntity adminRole = new UserRoleEntity();
        adminRole.setRole(roleEntity);
        adminRole.setUser(admin);
        adminRole.setCreatedBy("system");
        adminRole.setUpdatedBy("system");

        admin.getRoles().add(adminRole);
        userRepository.save(admin);
        log.info("Admin user seeded successfully {adminId={}} {email={}}", admin.getId(), adminConfig.getEmail());

    }
}
