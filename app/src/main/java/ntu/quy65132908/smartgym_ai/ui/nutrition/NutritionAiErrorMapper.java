package ntu.quy65132908.smartgym_ai.ui.nutrition;

import java.util.Locale;

final class NutritionAiErrorMapper {
    private NutritionAiErrorMapper() {}

    static String toUserMessage(Exception error) {
        String raw = error != null ? String.valueOf(error.getMessage()) : "";
        String normalized = raw.toLowerCase(Locale.ROOT);

        if ((error instanceof IllegalArgumentException || normalized.contains("json"))
                && (normalized.contains("ai response") || normalized.contains("days")
                || normalized.contains("meals") || normalized.contains("format"))) {
            return "AI trả về kế hoạch ăn chưa đúng định dạng. Vui lòng tạo lại.";
        }

        if (error instanceof IllegalStateException
                && (normalized.contains("not configured") || normalized.contains("api key"))) {
            return "AI chưa được cấu hình. Hãy kiểm tra DEEPSEEK_API_KEY trong local.properties.";
        }

        if (normalized.contains("http 401") || normalized.contains("http 403")
                || normalized.contains("invalid api key") || normalized.contains("authentication")
                || normalized.contains("unauthorized") || normalized.contains("invalid_api_key")) {
            return "API key không hợp lệ hoặc đã hết hạn. Hãy kiểm tra lại DEEPSEEK_API_KEY.";
        }

        if (normalized.contains("quota") || normalized.contains("rate limit")
                || normalized.contains("rate-limit") || normalized.contains("http 429")) {
            return "Đã hết hạn mức DeepSeek API cho khóa hiện tại. Vui lòng thử lại sau.";
        }

        if (normalized.contains("http 404") || normalized.contains("model_not_found")
                || normalized.contains("model not found")) {
            return "Model AI không tồn tại. Hãy kiểm tra tên model trong cấu hình.";
        }

        if (normalized.contains("network") || normalized.contains("timeout")
                || normalized.contains("unavailable") || normalized.contains("unable to resolve")
                || normalized.contains("connect")) {
            return "Không thể kết nối AI. Hãy kiểm tra mạng và thử lại.";
        }

        return "Không thể tạo kế hoạch ăn từ AI. Hãy thử lại sau.";
    }
}
