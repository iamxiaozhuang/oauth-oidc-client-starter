package io.github.oidcclient.client.internal;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static Map<String, Object> parseObject(String json) {
        Parser parser = new Parser(json);
        Map<String, Object> result = parser.readObject();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw new IllegalArgumentException("unexpected trailing JSON content");
        }
        return result;
    }

    public static String stringify(Map<String, ?> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escape(entry.getKey())).append('"').append(':');
            appendValue(builder, entry.getValue());
        }
        return builder.append('}').toString();
    }

    private static void appendValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
        } else if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
        } else {
            builder.append('"').append(escape(String.valueOf(value))).append('"');
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static final class Parser {
        private final String json;
        private int index;

        private Parser(String json) {
            this.json = json == null ? "" : json;
        }

        private Map<String, Object> readObject() {
            skipWhitespace();
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                String key = readString();
                skipWhitespace();
                expect(':');
                Object value = readValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return map;
                }
                expect(',');
            }
        }

        private Object readValue() {
            skipWhitespace();
            if (peek('"')) {
                return readString();
            }
            if (peek('{')) {
                return readObject();
            }
            if (match("true")) {
                return Boolean.TRUE;
            }
            if (match("false")) {
                return Boolean.FALSE;
            }
            if (match("null")) {
                return null;
            }
            return readNumber();
        }

        private String readString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!isEnd()) {
                char current = json.charAt(index++);
                if (current == '"') {
                    return builder.toString();
                }
                if (current == '\\') {
                    if (isEnd()) {
                        throw new IllegalArgumentException("invalid JSON escape");
                    }
                    char escaped = json.charAt(index++);
                    builder.append(readEscaped(escaped));
                } else {
                    builder.append(current);
                }
            }
            throw new IllegalArgumentException("unterminated JSON string");
        }

        private char readEscaped(char escaped) {
            switch (escaped) {
                case '"':
                case '\\':
                case '/':
                    return escaped;
                case 'b':
                    return '\b';
                case 'f':
                    return '\f';
                case 'n':
                    return '\n';
                case 'r':
                    return '\r';
                case 't':
                    return '\t';
                case 'u':
                    return readUnicodeEscape();
                default:
                    throw new IllegalArgumentException("unsupported JSON escape: " + escaped);
            }
        }

        private char readUnicodeEscape() {
            if (index + 4 > json.length()) {
                throw new IllegalArgumentException("invalid JSON unicode escape");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                int digit = Character.digit(json.charAt(index++), 16);
                if (digit < 0) {
                    throw new IllegalArgumentException("invalid JSON unicode escape");
                }
                value = (value << 4) + digit;
            }
            return (char) value;
        }

        private Number readNumber() {
            int start = index;
            while (!isEnd()) {
                char current = json.charAt(index);
                if ((current >= '0' && current <= '9') || current == '-' || current == '+'
                        || current == '.' || current == 'e' || current == 'E') {
                    index++;
                } else {
                    break;
                }
            }
            String raw = json.substring(start, index);
            if (raw.isBlank()) {
                throw new IllegalArgumentException("expected JSON value at " + index);
            }
            if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
                return Double.parseDouble(raw);
            }
            return Long.parseLong(raw);
        }

        private boolean match(String value) {
            if (json.startsWith(value, index)) {
                index += value.length();
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (isEnd() || json.charAt(index) != expected) {
                throw new IllegalArgumentException("expected '" + expected + "' at " + index);
            }
            index++;
        }

        private boolean peek(char value) {
            return !isEnd() && json.charAt(index) == value;
        }

        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }

        private boolean isEnd() {
            return index >= json.length();
        }
    }
}
