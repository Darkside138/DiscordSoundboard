package net.dirtydeeds.discordsoundboard.tts.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dirtydeeds.discordsoundboard.tts.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Fish Audio TTS provider implementation.
 * <p>
 * Activated when {@code tts.enabled=true} is set in application.properties.
 * Requires a valid {@code tts.fish-audio.api-key}.
 * </p>
 */
@Service
@ConditionalOnProperty(name = "tts.enabled", havingValue = "true")
public class FishAudioTtsService implements TtsService {

    private static final Logger LOG = LoggerFactory.getLogger(FishAudioTtsService.class);

    private static final String TTS_ENDPOINT = "https://api.fish.audio/v1/tts";
    private static final String MODEL_LIST_ENDPOINT = "https://api.fish.audio/model";

    private final FishAudioConfig config;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Simple page-level cache: key = "page:pageSize", value = cached result + timestamp
    private final java.util.concurrent.ConcurrentHashMap<String, CachedPage> pageCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private record CachedPage(TtsVoicePage page, long fetchedAt) {}

    public FishAudioTtsService(FishAudioConfig config) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public TtsResult generateAudio(TtsRequest request) throws TtsException {
        if (request.text() == null || request.text().isBlank()) {
            throw new TtsException("Text must not be empty");
        }

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("text", request.text());

            // Speaker / voice model
            String speakerId = (request.speakerId() != null && !request.speakerId().isBlank())
                    ? request.speakerId()
                    : config.getDefaultVoiceId();
            if (speakerId != null && !speakerId.isBlank()) {
                body.put("reference_id", speakerId);
            }

            // Audio format
            String format = (request.format() != null && !request.format().isBlank())
                    ? request.format()
                    : config.getDefaultFormat();
            body.put("format", format);
            body.put("latency", "normal");

            // model belongs in the request body, not as an HTTP header
            String model = config.getDefaultModel();
            if (model != null && !model.isBlank()) {
                body.put("model", model);
            }

            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(TTS_ENDPOINT))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<byte[]> response = httpClient.send(
                    httpRequest, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() != 200) {
                // Log the raw response body so we can see the actual error from Fish Audio
                String errorBody;
                try {
                    errorBody = new String(response.body(), java.nio.charset.StandardCharsets.UTF_8);
                } catch (Exception ex) {
                    errorBody = "(unreadable)";
                }
                LOG.error("Fish Audio TTS failed: HTTP {} — {}", response.statusCode(), errorBody);
                if (response.statusCode() == 401) {
                    throw new TtsException("Fish Audio authentication failed. Check tts.fish-audio.api-key.");
                }
                if (response.statusCode() == 402) {
                    throw new TtsException("Fish Audio API error (402): " + errorBody);
                }
                throw new TtsException("Fish Audio API returned HTTP " + response.statusCode() + ": " + errorBody);
            }

            byte[] audioBytes = response.body();
            if (audioBytes == null || audioBytes.length == 0) {
                throw new TtsException("Fish Audio returned an empty audio response.");
            }

            LOG.info("Generated {} bytes of TTS audio (format={})", audioBytes.length, format);
            return new TtsResult(audioBytes, format);

        } catch (TtsException e) {
            throw e;
        } catch (Exception e) {
            throw new TtsException("Failed to generate TTS audio: " + e.getMessage(), e);
        }
    }

    @Override
    public TtsVoicePage getAvailableVoices(int page, int pageSize, String title) throws TtsException {
        String normalizedTitle = (title == null) ? "" : title.trim();
        String cacheKey = page + ":" + pageSize + ":" + normalizedTitle;
        long now = System.currentTimeMillis();

        CachedPage cached = pageCache.get(cacheKey);
        if (cached != null && (now - cached.fetchedAt()) < config.getVoiceCacheTtlMs()) {
            return cached.page();
        }

        try {
            StringBuilder urlBuilder = new StringBuilder(MODEL_LIST_ENDPOINT)
                    .append("?page_size=").append(pageSize)
                    .append("&page_number=").append(page)
                    .append("&self=false");
            if (!normalizedTitle.isEmpty()) {
                urlBuilder.append("&title=").append(java.net.URLEncoder.encode(normalizedTitle,
                        java.nio.charset.StandardCharsets.UTF_8));
            }
            String url = urlBuilder.toString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOG.warn("Fish Audio model list returned HTTP {}", response.statusCode());
                return cached != null ? cached.page() : new TtsVoicePage(List.of(), page, 0, false);
            }

            TtsVoicePage result = parsePage(response.body(), page, pageSize);
            pageCache.put(cacheKey, new CachedPage(result, now));
            LOG.info("Fetched {} voices from Fish Audio (page {}, total {})",
                    result.voices().size(), page, result.total());
            return result;

        } catch (TtsException e) {
            throw e;
        } catch (Exception e) {
            LOG.warn("Failed to fetch Fish Audio voices: {}", e.getMessage());
            return cached != null ? cached.page() : new TtsVoicePage(List.of(), page, 0, false);
        }
    }

    private TtsVoicePage parsePage(String json, int page, int pageSize) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<TtsVoice> voices = new ArrayList<>();

        // Fish Audio returns { "items": [...], "total": N }
        JsonNode items = root.has("items") ? root.get("items") : root;
        int total = root.has("total") ? root.get("total").asInt(0) : 0;

        if (items.isArray()) {
            for (JsonNode item : items) {
                String id = textOrEmpty(item, "_id");
                if (id.isBlank()) continue;
                String name = textOrEmpty(item, "title");
                if (name.isBlank()) name = id;
                String description = textOrEmpty(item, "description");
                voices.add(new TtsVoice(id, name, description, getProviderName()));
            }
        }

        boolean hasMore = (long) page * pageSize < total;
        return new TtsVoicePage(voices, page, total, hasMore);
    }

    private String textOrEmpty(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText("") : "";
    }

    @Override
    public String getProviderName() {
        return "fish-audio";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}