package lab;

import com.fasterxml.jackson.databind.JsonNode;
import lab.LlmClient.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Minggu 1: structured output. Output model = input eksternal:
 * parse, validasi, dan retry dengan pesan error jika gagal.
 */
public class Step4Structured {

    static final int MAX_ATTEMPTS = 3;

    record Complaint(String orderId, String category, int quantity) {
    }

    public static void main(String[] args) {
        String email = "Halo, pesanan saya A-1001 datang 2 buah tapi layarnya retak semua. Mohon dibantu.";
        LlmClient llm = new LlmClient();

        List<Message> messages = new ArrayList<>(List.of(
                Message.system("""
                        Ekstrak keluhan pelanggan ke JSON dengan field persis:
                        {"order_id": string format A-0000, "category": "cacat" | "terlambat" | "salah_kirim" | "lainnya", "quantity": integer >= 1}
                        Balas hanya JSON."""),
                Message.user(email)));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw = llm.chatJson(messages, 0.0);
            System.out.println("Percobaan " + attempt + ": " + raw);
            try {
                Complaint complaint = parse(raw);
                System.out.println("Valid: " + complaint);
                return;
            } catch (IllegalArgumentException e) {
                System.out.println("Tidak valid: " + e.getMessage());
                messages.add(Message.assistant(raw));
                messages.add(Message.user("JSON tidak valid: " + e.getMessage() + ". Perbaiki dan balas hanya JSON."));
            }
        }
        System.out.println("Gagal mendapat output valid setelah " + MAX_ATTEMPTS + " percobaan.");
    }

    static Complaint parse(String raw) {
        JsonNode node;
        try {
            node = LlmClient.MAPPER.readTree(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("bukan JSON");
        }
        String orderId = node.path("order_id").asText("");
        if (!orderId.matches("A-\\d{4}")) {
            throw new IllegalArgumentException("order_id harus format A-0000, didapat '" + orderId + "'");
        }
        String category = node.path("category").asText("");
        if (!List.of("cacat", "terlambat", "salah_kirim", "lainnya").contains(category)) {
            throw new IllegalArgumentException("category tidak dikenal: '" + category + "'");
        }
        JsonNode quantity = node.path("quantity");
        if (!quantity.canConvertToInt() || quantity.asInt() < 1) {
            throw new IllegalArgumentException("quantity harus integer >= 1");
        }
        return new Complaint(orderId, category, quantity.asInt());
    }
}
