package ntu.quy65132908.smartgym_ai.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class DeepSeekClientTest {

    @Test
    public void buildRequestBody_addsJsonResponseFormatOnlyWhenRequested() throws Exception {
        DeepSeekClient client = new DeepSeekClient(
                "https://example.test/chat",
                "deepseek-test",
                1000,
                2048
        );

        JSONObject plainBody = new JSONObject(client.buildRequestBody("hello", false));
        JSONObject jsonBody = new JSONObject(client.buildRequestBody("json", true));

        assertEquals("deepseek-test", plainBody.getString("model"));
        assertFalse(plainBody.has("response_format"));
        assertEquals("hello", plainBody.getJSONArray("messages")
                .getJSONObject(0)
                .getString("content"));
        assertEquals("json_object", jsonBody.getJSONObject("response_format").getString("type"));
        assertEquals(2048, jsonBody.getInt("max_tokens"));
    }

    @Test
    public void parseResponseBody_returnsTrimmedContentOrEmptyFallback() throws Exception {
        assertEquals(
                "Plan",
                DeepSeekClient.parseResponseBody("{\"choices\":[{\"message\":{\"content\":\"  Plan  \"}}]}")
        );
        assertEquals(
                "Không có phản hồi",
                DeepSeekClient.parseResponseBody("{\"choices\":[]}")
        );
    }

    @Test
    public void extractErrorMessage_prefersNestedApiMessageAndTrimsLongBodies() {
        String error = "{\"error\":{\"message\":\"response_format is unsupported\"}}";

        assertEquals("response_format is unsupported", DeepSeekClient.extractErrorMessage(error));
        assertTrue(DeepSeekClient.isJsonOutputUnsupported(DeepSeekClient.extractErrorMessage(error)));

        StringBuilder longBody = new StringBuilder();
        for (int i = 0; i < 400; i++) {
            longBody.append('x');
        }
        assertEquals(303, DeepSeekClient.extractErrorMessage(longBody.toString()).length());
    }

    @Test
    public void isJsonOutputUnsupported_onlyMatchesCompatibilityErrors() {
        assertTrue(DeepSeekClient.isJsonOutputUnsupported("unsupported parameter: response_format"));
        assertTrue(DeepSeekClient.isJsonOutputUnsupported("not support json_object"));
        assertFalse(DeepSeekClient.isJsonOutputUnsupported("invalid api key"));
        assertFalse(DeepSeekClient.isJsonOutputUnsupported(null));
    }
}
