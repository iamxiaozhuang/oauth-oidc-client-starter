package io.github.oidcclient.authservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
class DemoUserConfiguration {
    @Bean
    UserDetailsService demoUserDetailsService() {
        return new InMemoryUserDetailsManager(User.withUsername("user")
                .password("{noop}password")
                .roles("USER")
                .build());
    }
}
