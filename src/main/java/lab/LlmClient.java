package lab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client chat untuk endpoint OpenAI-compatible (Ollama).
 * Hanya memakai java.net.http.HttpClient dan Jackson, tanpa framework.
 */
public class LlmClient {

    public static final String BASE_URL =
            System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1");
    public static final String DEFAULT_MODEL = "llama3.2:3b";

    static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String model;

    public LlmClient() {
        this(DEFAULT_MODEL);
    }

    public LlmClient(String model) {
        this.model = model;
    }

    /** Satu pesan percakapan. Field null tidak dikirim. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            String role,
            String content,
            @JsonProperty("tool_calls") List<ToolCall> toolCalls,
            @JsonProperty("tool_call_id") String toolCallId) {

        public static Message system(String content) {
            return new Message("system", content, null, null);
        }

        public static Message user(String content) {
            return new Message("user", content, null, null);
        }

        public static Message assistant(String content) {
            return new Message("assistant", content, null, null);
        }

        public static Message tool(String toolCallId, String content) {
            return new Message("tool", content, null, toolCallId);
        }

        public boolean hasToolCalls() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolCall(String id, String type, FunctionCall function) {
    }

    /** arguments berupa string JSON, sesuai format OpenAI. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FunctionCall(String name, String arguments) {
    }

    public String chat(List<Message> messages) {
        return chat(messages, 0.7);
    }

    public String chat(List<Message> messages, double temperature) {
        Map<String, Object> body = baseBody(messages, temperature);
        return send(body).content();
    }

    /** JSON mode: model diminta mengembalikan objek JSON. Tetap harus divalidasi oleh pemanggil. */
    public String chatJson(List<Message> messages, double temperature) {
        Map<String, Object> body = baseBody(messages, temperature);
        body.put("response_format", Map.of("type", "json_object"));
        return send(body).content();
    }

    /** Tool calling: mengembalikan pesan assistant, bisa berisi tool_calls atau jawaban akhir. */
    public Message chatWithTools(List<Message> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = baseBody(messages, 0.0);
        body.put("tools", tools);
        return send(body);
    }

    private Map<String, Object> baseBody(List<Message> messages, double temperature) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("stream", false);
        return body;
    }

    private Message send(Map<String, Object> body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(BASE_URL + "/chat/completions"))
                    .timeout(Duration.ofMinutes(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("LLM error " + response.statusCode() + ": " + response.body());
            }
            JsonNode message = MAPPER.readTree(response.body()).path("choices").path(0).path("message");
            if (message.isMissingNode()) {
                throw new IllegalStateException("Respons LLM tanpa choices[0].message: " + response.body());
            }
            return MAPPER.treeToValue(message, Message.class);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal memanggil LLM di " + BASE_URL, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Dibatalkan", e);
        }
    }

    /** Helper untuk membuat definisi tool (JSON schema) dengan ringkas. */
    public static Map<String, Object> tool(String name, String description,
                                           Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", name,
                        "description", description,
                        "parameters", Map.of(
                                "type", "object",
                                "properties", properties,
                                "required", required)));
    }

    public static Map<String, Object> param(String type, String description) {
        return Map.of("type", type, "description", description);
    }
}
