// Feature: gov-security-auth-system, Property 10
package com.gov.security.property;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gov.security.security.XssRequestWrapper;
import net.jqwik.api.*;
import org.assertj.core.api.Assertions;

/**
 * 属性测试：XSS 过滤转义完整性
 *
 * <p><b>Validates: Requirements 12.1, 12.2</b>
 */
class XssFilterPropertyTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // HTML 特殊字符集合
    private static final char[] SPECIAL_CHARS = {'<', '>', '"', '\'', '&'};

    /**
     * 生成包含至少一个 HTML 特殊字符的字符串
     */
    @Provide
    Arbitrary<String> stringsWithSpecialChars() {
        // 生成普通字符串前缀
        Arbitrary<String> prefix = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(20);

        // 随机选取一个特殊字符
        Arbitrary<Character> specialChar = Arbitraries.of('<', '>', '"', '\'', '&');

        // 生成普通字符串后缀
        Arbitrary<String> suffix = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(0)
                .ofMaxLength(20);

        return Combinators.combine(prefix, specialChar, suffix)
                .as((p, c, s) -> p + c + s);
    }

    /**
     * 生成任意字符串（可能包含或不包含特殊字符）
     */
    @Provide
    Arbitrary<String> anyStrings() {
        return Arbitraries.strings()
                .withChars('a', 'b', 'c', '<', '>', '"', '\'', '&', '1', '2', ' ')
                .ofMinLength(0)
                .ofMaxLength(50);
    }

    /**
     * Property 10: XSS 过滤转义完整性
     * - 经过 escapeHtml() 处理后，结果不包含原始 HTML 特殊字符
     * - 若输入包含特殊字符，输出应包含对应的 HTML 实体
     *
     * <p><b>Validates: Requirements 12.1</b>
     */
    // Feature: gov-security-auth-system, Property 10
    @Property(tries = 100)
    void specialCharsEscaped(@ForAll("stringsWithSpecialChars") String input) {
        String result = XssRequestWrapper.escapeHtml(input);

        // 结果不应包含原始 HTML 特殊字符
        Assertions.assertThat(result).doesNotContain("<");
        Assertions.assertThat(result).doesNotContain(">");
        Assertions.assertThat(result).doesNotContain("\"");
        Assertions.assertThat(result).doesNotContain("'");

        // 注意：&amp; 本身包含 &，所以需要验证 & 只以实体形式出现
        // 即结果中的 & 只能是 HTML 实体的一部分（&lt; &gt; &quot; &#x27; &amp;）
        if (result.contains("&")) {
            // 将所有合法实体替换掉后，不应再有 & 残留
            String withoutEntities = result
                    .replace("&lt;", "")
                    .replace("&gt;", "")
                    .replace("&quot;", "")
                    .replace("&#x27;", "")
                    .replace("&amp;", "");
            Assertions.assertThat(withoutEntities).doesNotContain("&");
        }

        // 验证特殊字符被正确替换为对应实体
        if (input.contains("<")) {
            Assertions.assertThat(result).contains("&lt;");
        }
        if (input.contains(">")) {
            Assertions.assertThat(result).contains("&gt;");
        }
        if (input.contains("\"")) {
            Assertions.assertThat(result).contains("&quot;");
        }
        if (input.contains("'")) {
            Assertions.assertThat(result).contains("&#x27;");
        }
        if (input.contains("&")) {
            Assertions.assertThat(result).contains("&amp;");
        }
    }

    /**
     * Property 11: JSON 递归 XSS 过滤
     * - sanitizeJsonNode() 递归转义所有字符串字段
     * - 非字符串字段（数字、布尔值、null）保持不变
     *
     * <p><b>Validates: Requirements 12.2</b>
     */
    // Feature: gov-security-auth-system, Property 11
    @Property(tries = 100)
    void jsonRecursiveEscape(@ForAll("jsonObjectsWithSpecialChars") JsonNode input) {
        JsonNode result = XssRequestWrapper.sanitizeJsonNode(input);

        // 所有字符串字段不应包含原始 HTML 特殊字符
        assertNoRawSpecialCharsInNode(result);

        // 非字符串字段应保持不变
        assertNonStringFieldsUnchanged(input, result);
    }

    /**
     * 生成包含特殊字符的 JSON 对象（含嵌套结构）
     */
    @Provide
    Arbitrary<JsonNode> jsonObjectsWithSpecialChars() {
        Arbitrary<String> dirtyString = Arbitraries.of(
                "<script>alert(1)</script>",
                "hello & world",
                "say \"hi\"",
                "it's a test",
                "<b>bold</b>",
                "a>b",
                "normal text",
                "mixed <>&\"' chars"
        );

        Arbitrary<Integer> numbers = Arbitraries.integers().between(0, 1000);
        Arbitrary<Boolean> booleans = Arbitraries.of(true, false);

        return Combinators.combine(dirtyString, dirtyString, numbers, booleans)
                .as((s1, s2, num, bool) -> {
                    ObjectNode root = OBJECT_MAPPER.createObjectNode();
                    root.put("name", s1);
                    root.put("description", s2);
                    root.put("count", num);
                    root.put("active", bool);

                    // 嵌套对象
                    ObjectNode nested = OBJECT_MAPPER.createObjectNode();
                    nested.put("value", s1);
                    nested.put("score", num);
                    root.set("nested", nested);

                    return (JsonNode) root;
                });
    }

    /**
     * 递归断言节点中所有字符串不含原始 HTML 特殊字符
     */
    private void assertNoRawSpecialCharsInNode(JsonNode node) {
        if (node.isTextual()) {
            String text = node.textValue();
            Assertions.assertThat(text).doesNotContain("<");
            Assertions.assertThat(text).doesNotContain(">");
            Assertions.assertThat(text).doesNotContain("\"");
            Assertions.assertThat(text).doesNotContain("'");
            // & 只能以实体形式出现
            if (text.contains("&")) {
                String withoutEntities = text
                        .replace("&lt;", "")
                        .replace("&gt;", "")
                        .replace("&quot;", "")
                        .replace("&#x27;", "")
                        .replace("&amp;", "");
                Assertions.assertThat(withoutEntities).doesNotContain("&");
            }
        } else if (node.isObject()) {
            node.fields().forEachRemaining(entry -> assertNoRawSpecialCharsInNode(entry.getValue()));
        } else if (node.isArray()) {
            node.forEach(this::assertNoRawSpecialCharsInNode);
        }
    }

    /**
     * 断言非字符串字段在处理前后保持不变
     */
    private void assertNonStringFieldsUnchanged(JsonNode original, JsonNode result) {
        if (original.isObject() && result.isObject()) {
            original.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode originalValue = entry.getValue();
                JsonNode resultValue = result.get(key);

                if (originalValue.isNumber()) {
                    Assertions.assertThat(resultValue.isNumber()).isTrue();
                    Assertions.assertThat(resultValue.numberValue()).isEqualTo(originalValue.numberValue());
                } else if (originalValue.isBoolean()) {
                    Assertions.assertThat(resultValue.isBoolean()).isTrue();
                    Assertions.assertThat(resultValue.booleanValue()).isEqualTo(originalValue.booleanValue());
                } else if (originalValue.isNull()) {
                    Assertions.assertThat(resultValue.isNull()).isTrue();
                } else if (originalValue.isObject()) {
                    assertNonStringFieldsUnchanged(originalValue, resultValue);
                }
            });
        }
    }
}
