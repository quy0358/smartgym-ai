package ntu.quy65132908.smartgym_ai.data.repository;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.data.model.User;
import ntu.quy65132908.smartgym_ai.util.GeminiKeyProvider;

@Singleton
public class GeminiRepository {
    private final GenerativeModelFutures model;

    @Inject
    public GeminiRepository(GeminiKeyProvider keyProvider) {
        GenerativeModel gm = new GenerativeModel("gemini-2.0-flash", keyProvider.getApiKey());
        this.model = GenerativeModelFutures.from(gm);
    }

    public void generateWorkoutPlan(User user, String goal, AiCallback cb) {
        StringBuilder p = new StringBuilder("Bạn là huấn luyện viên thể hình. Tạo kế hoạch 7 ngày:\n");
        if (user.getWeight() != null) p.append("- Cân nặng: ").append(user.getWeight()).append("kg\n");
        if (user.getHeight() != null) p.append("- Chiều cao: ").append(user.getHeight()).append("cm\n");
        if (user.getBmi() != null) p.append("- BMI: ").append(user.getBmi()).append("\n");
        p.append("- Mục tiêu: ").append(goal != null ? goal : "tăng cơ").append("\nTrả lời tiếng Việt, Markdown.");
        callGemini(p.toString(), cb);
    }

    public void analyzeForm(String exercise, String description, AiCallback cb) {
        String p = "Phân tích form bài \"" + exercise + "\": \"" + description + "\"\n" +
                "Cho: 1)Điểm đúng 2)Lỗi 3)Cách sửa 4)Mẹo an toàn. Tiếng Việt, Markdown.";
        callGemini(p, cb);
    }

    private void callGemini(String prompt, AiCallback cb) {
        Content content = new Content.Builder().addText(prompt).build();
        ListenableFuture<GenerateContentResponse> future = model.generateContent(content);
        Futures.addCallback(future, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse r) {
                String text = r.getText();
                cb.onSuccess(text != null ? text : "Không có phản hồi");
            }

            @Override
            public void onFailure(Throwable t) {
                cb.onError(new Exception(t));
            }
        }, Executors.newSingleThreadExecutor());
    }

    public interface AiCallback {
        void onSuccess(String response);
        void onError(Exception e);
    }
}
