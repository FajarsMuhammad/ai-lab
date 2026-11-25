package lab;

import com.fasterxml.jackson.databind.JsonNode;
import lab.LlmClient.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Week 1: structured output. Model output = external input:
 * parse, validate, and retry with the error message on failure.
 */
public class Step4Structured {

    static final int MAX_ATTEMPTS = 3;

    record Complaint(String orderId, String category, int quantity) {
    }

    public static void main(String[] args) {
        String email = "Hi, my order A-1001 arrived with 2 units but both screens are cracked. Please help.";
        LlmClient llm = new LlmClient();

        List<Message> messages = new ArrayList<>(List.of(
                Message.system("""
                        Extract the customer complaint into JSON with exactly these fields:
                        {"order_id": string in format A-0000, "category": "defective" | "late" | "wrong_item" | "other", "quantity": integer >= 1}
                        Reply with JSON only."""),
                Message.user(email)));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw = llm.chatJson(messages, 0.0);
            System.out.println("Attempt " + attempt + ": " + raw);
            try {
                Complaint complaint = parse(raw);
                System.out.println("Valid: " + complaint);
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid: " + e.getMessage());
                messages.add(Message.assistant(raw));
                messages.add(Message.user("Invalid JSON: " + e.getMessage() + ". Fix it and reply with JSON only."));
            }
        }
        System.out.println("No valid output after " + MAX_ATTEMPTS + " attempts.");
    }

    static Complaint parse(String raw) {
        JsonNode node;
        try {
            node = LlmClient.MAPPER.readTree(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("not JSON");
        }
        String orderId = node.path("order_id").asText("");
        if (!orderId.matches("A-\\d{4}")) {
            throw new IllegalArgumentException("order_id must be in format A-0000, got '" + orderId + "'");
        }
        String category = node.path("category").asText("");
        if (!List.of("defective", "late", "wrong_item", "other").contains(category)) {
            throw new IllegalArgumentException("unknown category: '" + category + "'");
        }
        JsonNode quantity = node.path("quantity");
        if (!quantity.canConvertToInt() || quantity.asInt() < 1) {
            throw new IllegalArgumentException("quantity must be an integer >= 1");
        }
        return new Complaint(orderId, category, quantity.asInt());
    }
}
