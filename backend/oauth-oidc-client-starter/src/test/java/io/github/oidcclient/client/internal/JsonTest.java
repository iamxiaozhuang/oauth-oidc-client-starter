package io.github.oidcclient.client.internal;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonTest {
    @Test
    void parseObjectDecodesUnicodeEscapesInStrings() {
        Map<String, Object> result = Json.parseObject("""
                {"sub":"123","name":"\\u5f20\\u4e09","picture":"https:\\/\\/example.com\\/a.png"}
                """);

        assertThat(result)
                .containsEntry("sub", "123")
                .containsEntry("name", "\u5f20\u4e09")
                .containsEntry("picture", "https://example.com/a.png");
    }

    @Test
    void parseObjectRejectsInvalidUnicodeEscapes() {
        assertThatThrownBy(() -> Json.parseObject("{\"name\":\"\\u12xz\"}"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("invalid JSON unicode escape");
    }
}
