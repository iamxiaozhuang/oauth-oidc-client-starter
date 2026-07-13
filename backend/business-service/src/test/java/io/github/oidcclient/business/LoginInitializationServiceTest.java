package io.github.oidcclient.business;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;

class LoginInitializationServiceTest {
    private final LoginInitializationService service = new LoginInitializationService();

    @Test
    void initializesTenantAndBusinessPermissionsForCurrentUser() {
        var authentication = new TestingAuthenticationToken(
                "user",
                null,
                "SCOPE_openid",
                "SCOPE_business.read"
        );

        var result = service.initialize(authentication);

        assertThat(result.initialized()).isTrue();
        assertThat(result.userId()).isEqualTo("user");
        assertThat(result.tenant().tenantId()).isEqualTo("demo-tenant");
        assertThat(result.tenant().tenantName()).isEqualTo("示例租户");
        assertThat(result.permissions()).containsExactly("business:read");
    }
}
