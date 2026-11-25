package lab.week5;

import com.fasterxml.jackson.databind.JsonNode;
import lab.eval.EvalSet;
import lab.llm.EmbeddingClient;
import lab.llm.LlmClient;
import lab.llm.LlmClient.Message;
import lab.rag.DocStore;
import lab.rag.RagService;
import lab.week4.Step8Rag;

import java.util.ArrayList;
import java.util.List;

/**
 * Week 5: LLM-as-judge. A second model call decides whether a RAG answer is grounded in the context
 * or contains hallucinations. The judge output is validated and retried too.
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
                    System.out.println("[REJECTED] " + c.question());
                    continue;
                }
                Verdict verdict = judge(llm, RagService.formatContext(answer.context()), c.question(), answer.text());
                if (verdict == null) {
                    judgeFailed++;
                    System.out.println("[JUDGE FAILED] " + c.question());
                } else if (verdict.grounded()) {
                    grounded++;
                    System.out.println("[GROUNDED] " + c.question());
                } else {
                    hallucinated++;
                    System.out.println("[HALLUCINATION] " + c.question() + "\n   answer: " + answer.text()
                            + "\n   reason: " + verdict.reason());
                }
            }
        }
        System.out.printf("%nGrounded: %d, hallucinated: %d, rejected without LLM: %d, judge failed: %d%n",
                grounded, hallucinated, skipped, judgeFailed);
    }

    static Verdict judge(LlmClient llm, String context, String question, String answer) {
        List<Message> messages = new ArrayList<>(List.of(
                Message.system("""
                        You are a grader. Decide whether the ANSWER is fully supported by the CONTEXT.
                        Any claim not found in the context = not grounded.
                        Reply with JSON only: {"grounded": true|false, "reason": "short reason"}"""),
                Message.user("CONTEXT:\n" + context + "\n\nQUESTION: " + question + "\n\nANSWER: " + answer)));

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String raw = llm.chatJson(messages, 0.0);
            try {
                JsonNode node = LlmClient.MAPPER.readTree(raw);
                if (!node.path("grounded").isBoolean()) {
                    throw new IllegalArgumentException("field grounded must be a boolean");
                }
                return new Verdict(node.path("grounded").asBoolean(), node.path("reason").asText(""));
            } catch (Exception e) {
                messages.add(Message.assistant(raw));
                messages.add(Message.user("Invalid output: " + e.getMessage() + ". Reply with JSON only, in the given format."));
            }
        }
        return null;
    }
}
