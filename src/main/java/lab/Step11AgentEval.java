package lab;

import java.util.HashSet;
import java.util.Set;

/**
 * Minggu 6: evaluasi agent. Apakah tool yang dipilih model benar untuk tiap pertanyaan?
 * Dibandingkan sebagai himpunan: urutan dan pemanggilan berulang tidak dihitung salah.
 */
public class Step11AgentEval {

    public static void main(String[] args) {
        EmbeddingClient embeddings = new EmbeddingClient();
        int correct = 0, missing = 0, extra = 0, gaveUp = 0;

        try (DocStore store = new DocStore()) {
            Step8Rag.ingest(store, embeddings, 400);
            ToolLoop loop = new ToolLoop(new LlmClient(), false);
            ToolLoop.ToolExecutor executor = Step11Agent.executor(embeddings, store);

            for (EvalSet.AgentCase c : EvalSet.AGENT_CASES) {
                ToolLoop.Result result = Step11Agent.ask(loop, executor, c.question());
                Set<String> actual = Set.copyOf(result.toolsCalled());

                Set<String> missingTools = new HashSet<>(c.expectedTools());
                missingTools.removeAll(actual);
                Set<String> extraTools = new HashSet<>(actual);
                extraTools.removeAll(c.expectedTools());

                boolean ok = missingTools.isEmpty() && extraTools.isEmpty();
                if (ok) {
                    correct++;
                }
                missing += missingTools.size();
                extra += extraTools.size();
                if (ToolLoop.GAVE_UP.equals(result.answer())) {
                    gaveUp++;
                }
                System.out.printf("[%s] %s%n      harapan: %s, aktual: %s%n",
                        ok ? "BENAR" : "SALAH", c.question(), c.expectedTools(), result.toolsCalled());
            }
        }
        int total = EvalSet.AGENT_CASES.size();
        System.out.printf("%nTool benar: %d/%d, tool terlewat: %d, tool tidak perlu: %d, kena MAX_STEPS: %d%n",
                correct, total, missing, extra, gaveUp);
    }
}
