package io.github.oidcclient.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class DemoUserConfigurationTest {
    @Test
    void registersDocumentedDemoUser() {
        UserDetailsService users = new DemoUserConfiguration().demoUserDetailsService();

        UserDetails user = users.loadUserByUsername("user");

        assertThat(user.getPassword()).isEqualTo("{noop}password");
        assertThat(user.getAuthorities()).extracting("authority").contains("ROLE_USER");
        assertThatNoException().isThrownBy(() -> users.loadUserByUsername("user"));
    }

    @Test
    void doesNotRegisterUnexpectedUsers() {
        UserDetailsService users = new DemoUserConfiguration().demoUserDetailsService();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> users.loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
