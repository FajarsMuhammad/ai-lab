package lab.rag;

import lab.llm.EmbeddingClient;
import lab.llm.LlmClient;
import lab.llm.LlmClient.Message;

import java.util.List;

/**
 * RAG: retrieve top-3, drop chunks below MIN_SIMILARITY, then answer with [n] citations.
 * If no context passes the threshold, the LLM is not called at all.
 */
public class RagService {

    public static final int TOP_K = 3;
    public static final double MIN_SIMILARITY = 0.5;
    public static final String NO_ANSWER = "Sorry, that information is not in our policy document.";

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

    /** Chunks that pass the threshold, most similar first. */
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
                        You are a customer service assistant for an online store.
                        Answer ONLY from the numbered context below. Every claim must carry a [n] citation.
                        If the context does not contain the answer, say: "%s"
                        Answer briefly.""".formatted(NO_ANSWER)),
                Message.user("Context:\n" + formatContext(context) + "\n\nQuestion: " + question));
        return new Answer(llm.chat(messages, 0.0), context);
    }

    /** Formats context as [1], [2], ...; also used by the search_policy tool in Step11Agent. */
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
