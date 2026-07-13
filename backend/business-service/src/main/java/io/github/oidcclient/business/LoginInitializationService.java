package io.github.oidcclient.business;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoginInitializationService {
    public LoginInitializationResult initialize(Authentication authentication) {
        String userId = authentication.getName();

        // Demo data: a real business system can load the user's tenant, roles, menus,
        // and data permissions from its own database or permission service here.
        TenantInfo tenant = new TenantInfo("demo-tenant", "示例租户");
        List<String> permissions = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("SCOPE_business."))
                .map(authority -> authority.substring("SCOPE_".length()).replace('.', ':'))
                .sorted()
                .toList();

        return new LoginInitializationResult(true, userId, tenant, permissions);
    }

    public record LoginInitializationResult(
            boolean initialized,
            String userId,
            TenantInfo tenant,
            List<String> permissions
    ) {
    }

    public record TenantInfo(String tenantId, String tenantName) {
    }
}
