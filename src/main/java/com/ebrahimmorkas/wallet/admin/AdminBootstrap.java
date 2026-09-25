package com.ebrahimmorkas.wallet.admin;

import com.ebrahimmorkas.wallet.user.Role;
import com.ebrahimmorkas.wallet.user.User;
import com.ebrahimmorkas.wallet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Creates the first administrator from {@code app.admin.email} / {@code app.admin.password} if no
 * account with that email exists. Disabled unless the email is configured.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty("app.admin.email")
class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = environment.getRequiredProperty("app.admin.email").trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            return;
        }
        String password = environment.getRequiredProperty("app.admin.password");
        userRepository.save(new User(email, passwordEncoder.encode(password), "Administrator", Role.ADMIN));
        log.info("Created bootstrap administrator {}", email);
    }
}
