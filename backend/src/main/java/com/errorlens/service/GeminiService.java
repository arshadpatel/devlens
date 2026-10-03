package com.errorlens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calls Gemma 4 through the Gemini API (generateContent) with an image + prompt
 * and returns the model's structured JSON answer.
 */
@Slf4j
@Service
public class GeminiService {

    private static final String PROMPT_TEMPLATE = """
            You are ErrorLens, a senior developer who explains error screenshots to beginners.
            Look carefully at the attached screenshot (terminal, browser console, IDE, or app dialog).
            %s

            Respond with ONLY a valid JSON object (no markdown fences, no prose) with exactly these keys:
            {
              "title": "short name of the error",
              "tool": "language / framework / tool you identified",
              "severity": "low" | "medium" | "high",
              "summary": "1-2 sentences, plain English, what went wrong",
              "root_cause": "the most likely underlying cause, 1-3 sentences",
              "evidence": ["exact text snippets you READ from the screenshot that support your diagnosis (max 3)"],
              "fix_steps": ["ordered, concrete steps the user should take (3-6)"],
              "commands": ["copy-pasteable shell commands or code lines, if relevant (can be empty)"],
              "prevention": "one tip to avoid this next time",
              "confidence": 0-100 integer
            }
            If the image is not an error, set title to "No error detected" and explain in summary.
            """;

    private final RestClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String defaultModel;
    private final Set<String> allowedModels;

    public GeminiService(RestClient geminiRestClient,
                         ObjectMapper mapper,
                         @Value("${gemini.api-key}") String apiKey,
                         @Value("${gemini.default-model}") String defaultModel,
                         @Value("${gemini.allowed-models}") String allowedModels) {

        this.client = geminiRestClient;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.defaultModel = defaultModel;
        this.allowedModels = new HashSet<>(Arrays.asList(allowedModels.split(",")));

        log.info("GeminiService initialized. defaultModel={}, allowedModels={}",
                defaultModel, allowedModels);
    }

    public String defaultModel() {
        return defaultModel;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public JsonNode explain(byte[] imageBytes,
                            String mimeType,
                            String context,
                            String requestedModel) {

        long startTime = System.currentTimeMillis();

        log.info("Gemini request started. requestedModel={}, mimeType={}, imageSize={} bytes",
                requestedModel, mimeType, imageBytes.length);

        if (!hasApiKey()) {
            log.error("Gemini API key is not configured.");

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "GEMINI_API_KEY is not set on the server."
            );
        }

        String model = (requestedModel == null || requestedModel.isBlank())
                ? defaultModel
                : requestedModel.trim();

        log.debug("Using Gemini model: {}", model);

        if (!allowedModels.contains(model)) {
            log.warn("Rejected Gemini model: {}. Allowed models: {}", model, allowedModels);

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Model not allowed: " + model
            );
        }

        String ctx = (context == null || context.isBlank())
                ? ""
                : "Extra context from the user: " + context.trim();

        String prompt = PROMPT_TEMPLATE.formatted(ctx);

        log.debug("Gemini prompt:\n{}", prompt);

        String encodedImage = Base64.getEncoder().encodeToString(imageBytes);

        log.debug("Preparing Gemini multimodal request. imageBase64Size={} characters",
                encodedImage.length());

        // Gemini API request body: one user turn = [image part, text part]
        Map<String, Object> imagePart = Map.of(
                "inline_data",
                Map.of(
                        "mime_type", mimeType,
                        "data", encodedImage
                )
        );

        Map<String, Object> textPart = Map.of(
                "text", prompt
        );

        Map<String, Object> body = Map.of(
                "contents",
                List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(imagePart, textPart)
                        )
                ),
                "generationConfig",
                Map.of(
                        "temperature", 0.2,
                        "maxOutputTokens", 2048
                )
        );

        log.info("Calling Gemini API. model={}, endpoint={}:generateContent",
                model, model);

        try {

            JsonNode response = client.post()
                    .uri("/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {

                        String detail = new String(
                                res.getBody().readAllBytes(),
                                StandardCharsets.UTF_8
                        );

                        String apiMessage = extractApiMessage(detail);

                        log.error(
                                "Gemini API returned an error. status={}, message={}",
                                res.getStatusCode().value(),
                                apiMessage
                        );

                        throw new ResponseStatusException(
                                HttpStatus.BAD_GATEWAY,
                                "Gemini API error "
                                        + res.getStatusCode().value()
                                        + ": "
                                        + apiMessage
                        );
                    })
                    .body(JsonNode.class);

            long duration = System.currentTimeMillis() - startTime;

            log.info("Gemini API response received successfully. duration={} ms",
                    duration);

            log.debug("Raw Gemini response:\n{}",
                    response == null ? "null" : response.toPrettyString());

            String modelText = extractText(response);

            log.debug("Extracted model text:\n{}", modelText);

            JsonNode result = parseModelJson(modelText);

            log.info("Gemini response parsed successfully. duration={} ms",
                    System.currentTimeMillis() - startTime);

            log.debug("Final parsed ErrorLens result:\n{}",
                    result.toPrettyString());

            return result;

        } catch (ResponseStatusException ex) {

            log.error(
                    "Gemini request failed. model={}, duration={} ms, status={}, message={}",
                    model,
                    System.currentTimeMillis() - startTime,
                    ex.getStatusCode(),
                    ex.getReason()
            );

            throw ex;

        } catch (Exception ex) {

            log.error(
                    "Unexpected error while calling Gemini. model={}, duration={} ms",
                    model,
                    System.currentTimeMillis() - startTime,
                    ex
            );

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unexpected error while calling Gemini."
            );
        }
    }

    /**
     * Concatenate text parts, skipping any "thought" parts.
     */
    private String extractText(JsonNode response) {

        log.debug("Extracting text from Gemini response.");

        StringBuilder sb = new StringBuilder();

        JsonNode parts = response == null
                ? null
                : response
                .path("candidates")
                .path(0)
                .path("content")
                .path("parts");

        if (parts != null && parts.isArray()) {

            log.debug("Gemini response contains {} content parts.",
                    parts.size());

            for (JsonNode p : parts) {

                if (p.hasNonNull("text")
                        && !p.path("thought").asBoolean(false)) {

                    sb.append(p.get("text").asText()).append("\n");
                }
            }
        }

        if (sb.length() == 0) {

            log.error("Gemini returned an empty usable response.");

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Empty response from model (possibly blocked). Try again."
            );
        }

        return sb.toString();
    }

    /**
     * Models sometimes wrap JSON in fences or add prose;
     * pull out the outermost {...}.
     */
    private JsonNode parseModelJson(String text) {

        log.debug("Parsing Gemini model output as JSON.");

        String t = text
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int s = t.indexOf('{');
        int e = t.lastIndexOf('}');

        if (s < 0 || e <= s) {

            log.error("Gemini model did not return a JSON object. Response:\n{}",
                    text);

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Model did not return JSON. Try again."
            );
        }

        try {

            JsonNode result = mapper.readTree(
                    t.substring(s, e + 1)
            );

            log.debug("Gemini JSON parsed successfully.");

            return result;

        } catch (Exception ex) {

            log.error("Failed to parse Gemini response as JSON.", ex);

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not parse model JSON. Try again."
            );
        }
    }

    private String extractApiMessage(String raw) {

        try {

            JsonNode n = mapper.readTree(raw);

            String message = n
                    .path("error")
                    .path("message")
                    .asText("");

            return message.isEmpty() ? raw : message;

        } catch (Exception ex) {

            log.debug("Could not parse Gemini error response as JSON.", ex);

            return raw;
        }
    }
}
