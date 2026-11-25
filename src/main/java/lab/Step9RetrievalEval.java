package lab;

import java.util.List;

/**
 * Minggu 5: evaluasi retrieval. Grid chunk size x threshold.
 * hit@1 / hit@3 untuk kasus yang bisa dijawab, reject untuk kasus yang tidak bisa dijawab.
 */
public class Step9RetrievalEval {

    static final int[] CHUNK_SIZES = {200, 400, 800};
    static final double[] THRESHOLDS = {0.4, 0.5, 0.6};

    public static void main(String[] args) {
        EmbeddingClient embeddings = new EmbeddingClient();
        long answerable = EvalSet.CASES.stream().filter(EvalSet.Case::answerable).count();
        long unanswerable = EvalSet.CASES.size() - answerable;

        System.out.printf("%-6s %-6s %-6s %-6s %-7s %-11s %-12s%n",
                "chunk", "thr", "hit@1", "hit@3", "reject", "salah-tolak", "salah-terima");
        try (DocStore store = new DocStore()) {
            for (int chunkSize : CHUNK_SIZES) {
                Step8Rag.ingest(store, embeddings, chunkSize);
                // Ambil hasil sekali per chunk size, lalu terapkan tiap threshold
                List<List<DocStore.Hit>> results = EvalSet.CASES.stream()
                        .map(c -> store.search(embeddings.embed(c.question()), RagService.TOP_K))
                        .toList();

                for (double threshold : THRESHOLDS) {
                    int hit1 = 0, hit3 = 0, reject = 0, falseReject = 0, falseAccept = 0;
                    for (int i = 0; i < EvalSet.CASES.size(); i++) {
                        EvalSet.Case c = EvalSet.CASES.get(i);
                        List<DocStore.Hit> kept = results.get(i).stream()
                                .filter(h -> h.similarity() >= threshold)
                                .toList();
                        if (c.answerable()) {
                            if (kept.isEmpty()) {
                                falseReject++;
                            } else {
                                if (kept.get(0).content().contains(c.expected())) {
                                    hit1++;
                                }
                                if (kept.stream().anyMatch(h -> h.content().contains(c.expected()))) {
                                    hit3++;
                                }
                            }
                        } else if (kept.isEmpty()) {
                            reject++;
                        } else {
                            falseAccept++;
                        }
                    }
                    System.out.printf("%-6d %-6.2f %-6s %-6s %-7s %-11d %-12d%n", chunkSize, threshold,
                            hit1 + "/" + answerable, hit3 + "/" + answerable, reject + "/" + unanswerable,
                            falseReject, falseAccept);
                }
            }
        }
    }
}
