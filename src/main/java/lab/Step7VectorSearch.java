package lab;

import java.util.List;

/** Week 3: store embeddings in pgvector, then search by cosine distance. */
public class Step7VectorSearch {

    public static void main(String[] args) {
        EmbeddingClient embeddings = new EmbeddingClient();
        List<String> faqs = List.of(
                "Returns can be requested up to 7 days after the item is received.",
                "Orders ship every day except Sunday.",
                "Electronic products have a 12-month warranty.",
                "We accept COD payments up to Rp2,000,000.");

        try (DocStore store = new DocStore()) {
            List<float[]> vectors = embeddings.embed(faqs);
            store.reset(vectors.get(0).length);
            for (int i = 0; i < faqs.size(); i++) {
                store.add("faq", faqs.get(i), vectors.get(i));
            }

            String query = "How long is the laptop warranty?";
            System.out.println("Query: " + query);
            for (DocStore.Hit hit : store.search(embeddings.embed(query), 3)) {
                System.out.printf("  %.3f  %s%n", hit.similarity(), hit.content());
            }
        }
    }
}
