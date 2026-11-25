package lab.week3;

import lab.llm.EmbeddingClient;

import java.util.List;

/** Week 3: embeddings and cosine similarity. Sentences with similar meaning have nearby vectors. */
public class Step6EmbeddingBasics {

    public static void main(String[] args) {
        EmbeddingClient client = new EmbeddingClient();
        List<String> sentences = List.of(
                "How do I send an item back?",
                "I want to return a product I bought.",
                "What is the shipping cost to Surabaya?",
                "My cat likes to sleep on the sofa.");
        List<float[]> vectors = client.embed(sentences);
        System.out.println("Vector dimension: " + vectors.get(0).length + "\n");

        for (int i = 1; i < sentences.size(); i++) {
            double sim = EmbeddingClient.cosine(vectors.get(0), vectors.get(i));
            System.out.printf("%.3f  \"%s\" vs \"%s\"%n", sim, sentences.get(0), sentences.get(i));
        }
    }
}
