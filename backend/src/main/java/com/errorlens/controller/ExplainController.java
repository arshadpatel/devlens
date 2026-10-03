package com.errorlens.controller;

import com.errorlens.service.GeminiService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ExplainController {

    private final GeminiService gemini;

    public ExplainController(GeminiService gemini) {
        this.gemini = gemini;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "model", gemini.defaultModel(),
                "apiKeyConfigured", gemini.hasApiKey());
    }

    /** multipart/form-data: image (file), context (optional text), model (optional) */
    @PostMapping(value = "/explain", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JsonNode explain(@RequestPart("image") MultipartFile image,
                            @RequestParam(value = "context", required = false) String context,
                            @RequestParam(value = "model", required = false) String model) throws IOException {
        String type = image.getContentType();
        if (image.isEmpty() || type == null || !type.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please upload an image file.");
        }
        return gemini.explain(image.getBytes(), type, context, model);
    }
}
