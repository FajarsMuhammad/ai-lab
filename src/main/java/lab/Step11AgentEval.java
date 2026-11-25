package lab;

import java.util.HashSet;
import java.util.Set;

/**
 * Week 6: agent eval. Did the model pick the right tools for each question?
 * Compared as sets: order and repeated calls are not counted as wrong.
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
                System.out.printf("[%s] %s%n      expected: %s, actual: %s%n",
                        ok ? "PASS" : "FAIL", c.question(), c.expectedTools(), result.toolsCalled());
            }
        }
        int total = EvalSet.AGENT_CASES.size();
        System.out.printf("%nCorrect tools: %d/%d, missed tools: %d, unneeded tools: %d, hit MAX_STEPS: %d%n",
                correct, total, missing, extra, gaveUp);
    }
}
