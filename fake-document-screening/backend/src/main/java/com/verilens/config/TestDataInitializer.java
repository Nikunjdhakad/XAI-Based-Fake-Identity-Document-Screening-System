package com.verilens.config;

import com.verilens.entity.Role;
import com.verilens.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Seeds default roles in the test profile since Flyway is disabled.
 * In production, roles are seeded by Flyway migration V7.
 */
@Configuration
@Profile("test")
public class TestDataInitializer {

    @Bean
    public CommandLineRunner seedTestRoles(RoleRepository roleRepository) {
        return args -> {
            if (roleRepository.findByName("USER").isEmpty()) {
                roleRepository.save(new Role("USER", "Standard user"));
            }
            if (roleRepository.findByName("ANALYST").isEmpty()) {
                roleRepository.save(new Role("ANALYST", "Analyst"));
            }
            if (roleRepository.findByName("ADMIN").isEmpty()) {
                roleRepository.save(new Role("ADMIN", "Administrator"));
            }
        };
    }
}
