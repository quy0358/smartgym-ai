package ntu.quy65132908.smartgym_ai.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.BuildConfig;

@Singleton
public class DeepSeekKeyProvider {
    private final String apiKey;

    @Inject
    public DeepSeekKeyProvider() {
        this(BuildConfig.DEEPSEEK_API_KEY);
    }

    DeepSeekKeyProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey == null ? "" : apiKey.trim();
    }

    public boolean hasApiKey() {
        return !getApiKey().isEmpty();
    }
}
