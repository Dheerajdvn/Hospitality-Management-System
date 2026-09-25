package com.hospitality.auth.config;

import com.hospitality.auth.entity.Role;
import com.hospitality.auth.entity.RoleName;
import com.hospitality.auth.entity.User;
import com.hospitality.auth.repository.RoleRepository;
import com.hospitality.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Bootstrapping default roles and accounts in hms_auth_db...");

        Role customerRole = initRole(RoleName.ROLE_CUSTOMER, "Standard guest and customer role");
        Role adminRole = initRole(RoleName.ROLE_ADMIN, "Platform administrator with full management rights");
        Role staffRole = initRole(RoleName.ROLE_STAFF, "Hotel operations, front-desk, and housekeeping staff");

        // Seed default Admin user
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@hospitality.com")
                    .password(passwordEncoder.encode("admin123"))
                    .firstName("System")
                    .lastName("Administrator")
                    .isActive(true)
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);
            log.info("Default ADMIN user created: admin / admin123");
        }

        // Seed default Staff user
        if (!userRepository.existsByUsername("staff")) {
            User staff = User.builder()
                    .username("staff")
                    .email("staff@hospitality.com")
                    .password(passwordEncoder.encode("staff123"))
                    .firstName("Operations")
                    .lastName("Staff")
                    .isActive(true)
                    .roles(Set.of(staffRole))
                    .build();
            userRepository.save(staff);
            log.info("Default STAFF user created: staff / staff123");
        }

        // Seed default Customer user
        if (!userRepository.existsByUsername("customer")) {
            User customer = User.builder()
                    .username("customer")
                    .email("customer@hospitality.com")
                    .password(passwordEncoder.encode("customer123"))
                    .firstName("John")
                    .lastName("Doe")
                    .isActive(true)
                    .roles(Set.of(customerRole))
                    .build();
            userRepository.save(customer);
            log.info("Default CUSTOMER user created: customer / customer123");
        }
    }

    private Role initRole(RoleName roleName, String description) {
        return roleRepository.findByName(roleName).orElseGet(() -> {
            Role role = Role.builder()
                    .name(roleName)
                    .description(description)
                    .build();
            return roleRepository.save(role);
        });
    }
}
