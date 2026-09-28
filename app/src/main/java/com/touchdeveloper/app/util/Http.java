package com.touchdeveloper.app.util;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal HTTP client built on HttpURLConnection.
 *
 * Authorization headers are only attached when a caller explicitly supplies a
 * token, and no request or response headers are ever written to logs, so tokens
 * cannot leak into build logs or handoff packages.
 */
public final class Http {

    public static class Response {
        public final int code;
        public final String body;

        Response(int code, String body) {
            this.code = code;
            this.body = body;
        }

        public boolean ok() {
            return code >= 200 && code < 300;
        }
    }

    private Http() {
    }

    public static Response request(String method, String url, String token, String body,
                                   String contentType) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            conn.setRequestProperty("User-Agent", "TouchDeveloper/1.0");
            if (token != null && !token.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (body != null) {
                conn.setDoOutput(true);
                if (contentType != null) {
                    conn.setRequestProperty("Content-Type", contentType);
                }
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = conn.getResponseCode();
            InputStream stream = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String responseBody = readFully(stream);
            return new Response(code, responseBody);
        } catch (Exception e) {
            return new Response(-1, "Network error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public static Response get(String url, String token) {
        return request("GET", url, token, null, null);
    }

    public static Response put(String url, String token, String jsonBody) {
        return request("PUT", url, token, jsonBody, "application/json");
    }

    public static Response post(String url, String token, String jsonBody) {
        return request("POST", url, token, jsonBody, "application/json");
    }

    public static Response delete(String url, String token, String jsonBody) {
        return request("DELETE", url, token, jsonBody, "application/json");
    }

    private static String readFully(InputStream in) {
        if (in == null) {
            return "";
        }
        try (InputStream stream = in) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return new String(bos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    /** Extracts a JSON string field from a flat object body. */
    public static String stringField(String json, String field) {
        if (json == null) {
            return null;
        }
        String needle = "\"" + field + "\"";
        int key = json.indexOf(needle);
        if (key < 0) {
            return null;
        }
        int colon = json.indexOf(':', key + needle.length());
        if (colon < 0) {
            return null;
        }
        int start = json.indexOf('"', colon + 1);
        if (start < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(++i);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(next); break;
                }
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    public static Map<String, String> emptyQuery() {
        return new LinkedHashMap<>();
    }
}
