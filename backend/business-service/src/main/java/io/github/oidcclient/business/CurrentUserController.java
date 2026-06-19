package io.github.oidcclient.business;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class CurrentUserController {
    @GetMapping("/api/current-user")
    public Map<String, Object> currentUser(Authentication authentication) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", authentication.getName());

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            body.put("claims", jwt.getClaims());
            body.put("scopes", jwt.getClaimAsStringList("scope"));
        }

        return body;
    }

    @PostMapping("/api/auth/init")
    public Map<String, Object> initCurrentUser(Authentication authentication) {
        Map<String, Object> body = currentUser(authentication);
        body.put("initialized", true);
        return body;
    }
}
