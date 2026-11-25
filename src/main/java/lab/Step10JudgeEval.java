package lab;

import com.fasterxml.jackson.databind.JsonNode;
import lab.LlmClient.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Minggu 5: LLM-as-judge. Model kedua menilai apakah jawaban RAG grounded pada konteks
 * atau mengandung halusinasi. Output judge juga divalidasi dan di-retry.
 */
public class Step10JudgeEval {

    static final int MAX_ATTEMPTS = 3;

    record Verdict(boolean grounded, String reason) {
    }

    public static void main(String[] args) {
        LlmClient llm = new LlmClient();
        EmbeddingClient embeddings = new EmbeddingClient();
        int grounded = 0, hallucinated = 0, skipped = 0, judgeFailed = 0;

        try (DocStore store = new DocStore()) {
            Step8Rag.ingest(store, embeddings, 400);
            RagService rag = new RagService(llm, embeddings, store);

            for (EvalSet.Case c : EvalSet.CASES) {
                RagService.Answer answer = rag.answer(c.question());
                if (answer.context().isEmpty()) {
                    skipped++;
                    System.out.println("[DITOLAK] " + c.question());
                    continue;
                }
                Verdict verdict = judge(llm, RagService.formatContext(answer.context()), c.question(), answer.text());
                if (verdict == null) {
                    judgeFailed++;
                    System.out.println("[JUDGE GAGAL] " + c.question());
                } else if (verdict.grounded()) {
                    grounded++;
                    System.out.println("[GROUNDED] " + c.question());
                } else {
                    hallucinated++;
                    System.out.println("[HALUSINASI] " + c.question() + "\n   jawaban: " + answer.text()
                            + "\n   alasan: " + verdict.reason());
                }
            }
        }
        System.out.printf("%nGrounded: %d, halusinasi: %d, ditolak tanpa LLM: %d, judge gagal: %d%n",
                grounded, hallucinated, skipped, judgeFailed);
    }

    static Verdict judge(LlmClient llm, String context, String question, String answer) {
        List<Message> messages = new ArrayList<>(List.of(
                Message.system("""
                        Kamu penilai. Tentukan apakah JAWABAN sepenuhnya didukung oleh KONTEKS.
                        Klaim apa pun yang tidak ada di konteks = tidak grounded.
                        Balas hanya JSON: {"grounded": true|false, "reason": "alasan singkat"}"""),
                Message.user("KONTEKS:\n" + context + "\n\nPERTANYAAN: " + question + "\n\nJAWABAN: " + answer)));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw = llm.chatJson(messages, 0.0);
            try {
                JsonNode node = LlmClient.MAPPER.readTree(raw);
                if (!node.path("grounded").isBoolean()) {
                    throw new IllegalArgumentException("field grounded harus boolean");
                }
                return new Verdict(node.path("grounded").asBoolean(), node.path("reason").asText(""));
            } catch (Exception e) {
                messages.add(Message.assistant(raw));
                messages.add(Message.user("Output tidak valid: " + e.getMessage() + ". Balas hanya JSON sesuai format."));
            }
        }
        return null;
    }
}
