package com.studyassistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Typed configuration for the AI provider layer.
 *
 * <p>All values are bound from the {@code app.ai.*} namespace in
 * {@code application.properties}. The API key is intentionally read from
 * a property (never hardcoded) so it can be supplied via an environment
 * variable in production:
 *
 * <pre>
 * APP_AI_GEMINI_API-KEY=your-key-here mvn spring-boot:run
 * </pre>
 *
 * or via {@code application-local.properties} locally (git-ignored).
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiConfiguration {

    /** Active provider identifier. Supported values: {@code gemini}. */
    private String provider = "gemini";

    private Gemini gemini = new Gemini();

    public String getProvider() { return provider; }
    public void   setProvider(String provider) { this.provider = provider; }

    public Gemini getGemini() { return gemini; }
    public void   setGemini(Gemini gemini) { this.gemini = gemini; }

    public static class Gemini {

        /**
         * Google Gemini API key.
         * Set via {@code app.ai.gemini.api-key} in application-local.properties
         * or the {@code APP_AI_GEMINI_API-KEY} environment variable.
         * Never commit a real key to source control.
         */
        private String apiKey = "";

        /** Gemini model to use. Default: gemini-2.5-flash */
        private String model = "gemini-2.5-flash";

        /** Gemini REST API base URL. Overridable for testing. */
        private String baseUrl = "https://generativelanguage.googleapis.com";

        public String getApiKey()  { return apiKey; }
        public void   setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel()   { return model; }
        public void   setModel(String model) { this.model = model; }

        public String getBaseUrl() { return baseUrl; }
        public void   setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    }
}
