package ntu.quy65132908.smartgym_ai.data.repository;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class DeepSeekClient {
    private static final String EMPTY_RESPONSE = "Không có phản hồi";

    private final String apiUrl;
    private final String model;
    private final int timeoutMs;
    private final int maxTokens;

    DeepSeekClient(String apiUrl, String model, int timeoutMs, int maxTokens) {
        this.apiUrl = apiUrl;
        this.model = model;
        this.timeoutMs = timeoutMs;
        this.maxTokens = maxTokens;
    }

    String request(String apiKey, String prompt, boolean jsonOutput) throws Exception {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "application/json");

            byte[] requestBytes = buildRequestBody(prompt, jsonOutput).getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBytes);
            }

            int statusCode = connection.getResponseCode();
            InputStream responseStream = statusCode >= 200 && statusCode < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String responseBody = readStream(responseStream);

            if (statusCode < 200 || statusCode >= 300) {
                String errorMessage = extractErrorMessage(responseBody);
                if (jsonOutput && isJsonOutputUnsupported(errorMessage)) {
                    return request(apiKey, prompt, false);
                }
                throw new IOException("DeepSeek API error HTTP " + statusCode + ": " + errorMessage);
            }

            return parseResponseBody(responseBody);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    String buildRequestBody(String prompt) throws JSONException {
        return buildRequestBody(prompt, false);
    }

    String buildRequestBody(String prompt, boolean jsonOutput) throws JSONException {
        JSONObject message = new JSONObject()
                .put("role", "user")
                .put("content", prompt);
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("stream", false)
                .put("messages", new JSONArray().put(message));
        if (jsonOutput) {
            body.put("response_format", new JSONObject().put("type", "json_object"));
            body.put("max_tokens", maxTokens);
        }
        return body.toString();
    }

    static String parseResponseBody(String responseBody) throws JSONException {
        JSONObject root = new JSONObject(responseBody);
        JSONArray choices = root.optJSONArray("choices");
        if (choices == null || choices.length() == 0) {
            return EMPTY_RESPONSE;
        }

        JSONObject message = choices.optJSONObject(0) != null
                ? choices.optJSONObject(0).optJSONObject("message")
                : null;
        String content = message != null ? message.optString("content", "") : "";
        return content.trim().isEmpty() ? EMPTY_RESPONSE : content.trim();
    }

    static String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.trim().isEmpty()) {
            return "empty response";
        }

        try {
            JSONObject error = new JSONObject(responseBody).optJSONObject("error");
            if (error != null) {
                String message = error.optString("message", "");
                if (!message.trim().isEmpty()) {
                    return trimErrorMessage(message);
                }
            }
        } catch (Exception ignored) {
            // Keep the raw body when the API returns a non-JSON error.
        }
        return trimErrorMessage(responseBody);
    }

    static boolean isJsonOutputUnsupported(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }
        String normalized = errorMessage.toLowerCase(Locale.ROOT);
        return normalized.contains("response_format")
                || normalized.contains("json_object")
                || normalized.contains("unsupported parameter")
                || normalized.contains("not support json");
    }

    private static String trimErrorMessage(String value) {
        String trimmed = value != null ? value.trim() : "";
        if (trimmed.length() <= 300) {
            return trimmed;
        }
        return trimmed.substring(0, 300) + "...";
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }
}
