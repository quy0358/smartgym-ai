package ntu.quy65132908.smartgym_ai.ui.analysis;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AiErrorMapperTest {

    @Test
    public void toUserMessage_quotaExceeded_returnsVietnameseActionableMessage() {
        Exception error = new Exception(
                "com.google.ai.client.generativeai.type.QuotaExceededException: You exceeded your current quota. "
                        + "Please retry in 37.9s.");

        String message = AiErrorMapper.toUserMessage(error);

        assertTrue(message.contains("hạn mức"));
        assertTrue(message.contains("thử lại sau"));
        assertFalse(message.contains("com.google"));
        assertFalse(message.contains("QuotaExceededException"));
    }

    @Test
    public void toUserMessage_missingApiKey_returnsConfigurationMessage() {
        Exception error = new IllegalStateException("DeepSeek API key is not configured.");

        String message = AiErrorMapper.toUserMessage(error);

        assertTrue(message.contains("DEEPSEEK_API_KEY"));
        assertTrue(message.contains("chưa được cấu hình"));
    }

    @Test
    public void toUserMessage_responseFormatUnsupported_returnsProviderMessage() {
        Exception error = new Exception("HTTP 400: unsupported parameter response_format json_object");

        String message = AiErrorMapper.toUserMessage(error);

        assertTrue(message.contains("Nhà cung cấp AI"));
        assertTrue(message.contains("JSON"));
    }

    @Test
    public void toUserMessage_unknownError_returnsGenericMessage() {
        Exception error = new Exception("Something internal");

        String message = AiErrorMapper.toUserMessage(error);

        assertTrue(message.contains("Không thể kết nối AI"));
    }
}
