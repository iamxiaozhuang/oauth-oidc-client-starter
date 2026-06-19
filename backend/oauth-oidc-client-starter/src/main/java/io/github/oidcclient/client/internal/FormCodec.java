package io.github.oidcclient.client.internal;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

public final class FormCodec {
    private FormCodec() {
    }

    public static String encode(Map<String, String> values) {
        StringJoiner joiner = new StringJoiner("&");
        values.forEach((key, value) -> joiner.add(urlEncode(key) + "=" + urlEncode(value)));
        return joiner.toString();
    }

    public static URI appendQuery(URI uri, Map<String, String> query) {
        String existing = uri.getRawQuery();
        String combined = existing == null || existing.isBlank()
                ? encode(query)
                : existing + "&" + encode(query);
        return URI.create(uri.toString().split("\\?", 2)[0] + "?" + combined);
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
