package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.user.user.AppUser;
import com.worldcup.hotelbooking.user.user.AppUserRepository;
import com.worldcup.hotelbooking.user.user.Role;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
@Profile("!test")
public class UserSeeder {

    @Bean
    CommandLineRunner seedUsers(AppUserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Create admin user if not exists
            if (userRepository.findByUsername("admin").isEmpty()) {
                AppUser admin = new AppUser();
                admin.setUsername("admin");
                admin.setEmail("admin@example.com");
                admin.setPassword(passwordEncoder.encode("Admin@123"));
                admin.setRoles(Set.of(Role.ADMIN));
                admin.setEnabled(true);
                userRepository.save(admin);
                System.out.println("Seeded admin user");
            }

            // Create manager user if not exists
            if (userRepository.findByUsername("manager").isEmpty()) {
                AppUser manager = new AppUser();
                manager.setUsername("manager");
                manager.setEmail("manager@example.com");
                manager.setPassword(passwordEncoder.encode("Manager@123"));
                manager.setRoles(Set.of(Role.MANAGER));
                manager.setEnabled(true);
                userRepository.save(manager);
                System.out.println("Seeded manager user");
            }

            // Create guest user if not exists
            if (userRepository.findByUsername("guest").isEmpty()) {
                AppUser guest = new AppUser();
                guest.setUsername("guest");
                guest.setEmail("guest@example.com");
                guest.setPassword(passwordEncoder.encode("Guest@123"));
                guest.setRoles(Set.of(Role.GUEST));
                guest.setEnabled(true);
                userRepository.save(guest);
                System.out.println("Seeded guest user");
            }
        };
    }
}