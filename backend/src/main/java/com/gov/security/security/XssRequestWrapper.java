package com.gov.security.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * XSS 请求包装器
 * 继承 HttpServletRequestWrapper，对请求参数与请求体中的 HTML 特殊字符进行转义
 * 需求：12.1、12.2
 */
public class XssRequestWrapper extends HttpServletRequestWrapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final byte[] sanitizedBody;

    public XssRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        byte[] rawBody = request.getInputStream().readAllBytes();
        this.sanitizedBody = sanitizeBody(request, rawBody);
    }

    /**
     * 转义 HTML 特殊字符：<, >, ", ', &
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * 递归处理 JSON 节点中的所有字符串字段
     */
    public static JsonNode sanitizeJsonNode(JsonNode node) {
        if (node.isTextual()) {
            return new TextNode(escapeHtml(node.textValue()));
        } else if (node.isObject()) {
            ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
            node.fields().forEachRemaining(entry ->
                    objectNode.set(entry.getKey(), sanitizeJsonNode(entry.getValue()))
            );
            return objectNode;
        } else if (node.isArray()) {
            ArrayNode arrayNode = OBJECT_MAPPER.createArrayNode();
            node.forEach(element -> arrayNode.add(sanitizeJsonNode(element)));
            return arrayNode;
        }
        // 数字、布尔值、null 保持不变
        return node;
    }

    private byte[] sanitizeBody(HttpServletRequest request, byte[] rawBody) throws IOException {
        if (rawBody == null || rawBody.length == 0) {
            return rawBody;
        }
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            String bodyStr = new String(rawBody, StandardCharsets.UTF_8);
            try {
                JsonNode root = OBJECT_MAPPER.readTree(bodyStr);
                JsonNode sanitized = sanitizeJsonNode(root);
                return OBJECT_MAPPER.writeValueAsBytes(sanitized);
            } catch (Exception e) {
                // 非合法 JSON，原样返回
                return rawBody;
            }
        }
        return rawBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream bais = new ByteArrayInputStream(sanitizedBody);
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return bais.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // no-op
            }

            @Override
            public int read() {
                return bais.read();
            }
        };
    }

    @Override
    public String getParameter(String name) {
        String value = super.getParameter(name);
        return escapeHtml(value);
    }

    @Override
    public String[] getParameterValues(String name) {
        String[] values = super.getParameterValues(name);
        if (values == null) {
            return null;
        }
        String[] sanitized = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sanitized[i] = escapeHtml(values[i]);
        }
        return sanitized;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> original = super.getParameterMap();
        Map<String, String[]> sanitized = new HashMap<>();
        for (Map.Entry<String, String[]> entry : original.entrySet()) {
            String[] values = entry.getValue();
            String[] sanitizedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitizedValues[i] = escapeHtml(values[i]);
            }
            sanitized.put(entry.getKey(), sanitizedValues);
        }
        return sanitized;
    }
}
