package ntu.quy65132908.smartgym_ai.util;

import javax.inject.Inject;
import javax.inject.Singleton;

import ntu.quy65132908.smartgym_ai.BuildConfig;

@Singleton
public class GeminiKeyProvider {
    @Inject
    public GeminiKeyProvider() {}

    public String getApiKey() {
        return BuildConfig.GEMINI_API_KEY;
    }
}
