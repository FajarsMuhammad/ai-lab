package lab;

import lab.LlmClient.Message;

import java.util.List;

/**
 * RAG: retrieve top-3, buang chunk di bawah MIN_SIMILARITY, lalu jawab dengan sitasi [n].
 * Jika tidak ada konteks yang lolos, LLM tidak dipanggil sama sekali.
 */
public class RagService {

    public static final int TOP_K = 3;
    public static final double MIN_SIMILARITY = 0.5;
    public static final String NO_ANSWER = "Maaf, informasi itu tidak ada di dokumen kebijakan kami.";

    public record Answer(String text, List<DocStore.Hit> context) {
    }

    private final LlmClient llm;
    private final EmbeddingClient embeddings;
    private final DocStore store;
    private final double minSimilarity;

    public RagService(LlmClient llm, EmbeddingClient embeddings, DocStore store) {
        this(llm, embeddings, store, MIN_SIMILARITY);
    }

    public RagService(LlmClient llm, EmbeddingClient embeddings, DocStore store, double minSimilarity) {
        this.llm = llm;
        this.embeddings = embeddings;
        this.store = store;
        this.minSimilarity = minSimilarity;
    }

    /** Chunk yang lolos threshold, urut dari paling mirip. */
    public List<DocStore.Hit> retrieve(String question) {
        return store.search(embeddings.embed(question), TOP_K).stream()
                .filter(hit -> hit.similarity() >= minSimilarity)
                .toList();
    }

    public Answer answer(String question) {
        List<DocStore.Hit> context = retrieve(question);
        if (context.isEmpty()) {
            return new Answer(NO_ANSWER, context);
        }
        List<Message> messages = List.of(
                Message.system("""
                        Kamu asisten layanan pelanggan toko online.
                        Jawab HANYA berdasarkan konteks bernomor di bawah. Setiap klaim harus diberi sitasi [n].
                        Jika konteks tidak memuat jawabannya, katakan: "%s"
                        Jawab singkat dalam bahasa Indonesia.""".formatted(NO_ANSWER)),
                Message.user("Konteks:\n" + formatContext(context) + "\n\nPertanyaan: " + question));
        return new Answer(llm.chat(messages, 0.0), context);
    }

    /** Format konteks bernomor [1], [2], ... dipakai juga oleh tool search_policy di Step11Agent. */
    public static String formatContext(List<DocStore.Hit> hits) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hits.size(); i++) {
            DocStore.Hit hit = hits.get(i);
            sb.append('[').append(i + 1).append("] (").append(hit.source()).append(") ")
                    .append(hit.content()).append('\n');
        }
        return sb.toString().strip();
    }
}
