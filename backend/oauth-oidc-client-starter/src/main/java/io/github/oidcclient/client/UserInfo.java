package io.github.oidcclient.client;

import java.util.Map;

public final class UserInfo {
    private final String subject;
    private final String name;
    private final String email;
    private final Map<String, Object> claims;

    public UserInfo(String subject, String name, String email, Map<String, Object> claims) {
        this.subject = subject;
        this.name = name;
        this.email = email;
        this.claims = claims;
    }

    public String subject() {
        return subject;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public Map<String, Object> claims() {
        return claims;
    }
}
