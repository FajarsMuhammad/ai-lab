package lab.llm;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Embedding client for the OpenAI-compatible /embeddings endpoint. */
public class EmbeddingClient {

    public static final String DEFAULT_MODEL = "nomic-embed-text";
    /** Dimension of nomic-embed-text. bge-m3 = 1024. */
    public static final int DEFAULT_DIMENSION = 768;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String model;

    public EmbeddingClient() {
        this(DEFAULT_MODEL);
    }

    public EmbeddingClient(String model) {
        this.model = model;
    }

    public float[] embed(String text) {
        return embed(List.of(text)).get(0);
    }

    /** Batch embedding. Results are ordered by the index field so they match the input order. */
    public List<float[]> embed(List<String> texts) {
        try {
            String body = LlmClient.MAPPER.writeValueAsString(Map.of("model", model, "input", texts));
            HttpRequest request = HttpRequest.newBuilder(URI.create(LlmClient.BASE_URL + "/embeddings"))
                    .timeout(Duration.ofMinutes(2))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Embedding error " + response.statusCode() + ": " + response.body());
            }
            JsonNode data = LlmClient.MAPPER.readTree(response.body()).path("data");
            float[][] result = new float[texts.size()][];
            for (JsonNode item : data) {
                JsonNode vector = item.path("embedding");
                float[] v = new float[vector.size()];
                for (int i = 0; i < v.length; i++) {
                    v[i] = (float) vector.get(i).asDouble();
                }
                result[item.path("index").asInt()] = v;
            }
            if (Arrays.stream(result).anyMatch(v -> v == null)) {
                throw new IllegalStateException("Number of embeddings does not match number of inputs");
            }
            return new ArrayList<>(Arrays.asList(result));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call embeddings at " + LlmClient.BASE_URL, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted", e);
        }
    }

    public static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
