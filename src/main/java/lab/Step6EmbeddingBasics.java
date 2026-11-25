package lab;

import java.util.List;

/** Minggu 3: embedding dan cosine similarity. Kalimat bermakna mirip punya vektor yang berdekatan. */
public class Step6EmbeddingBasics {

    public static void main(String[] args) {
        EmbeddingClient client = new EmbeddingClient();
        List<String> sentences = List.of(
                "Bagaimana cara mengembalikan barang?",
                "Saya ingin retur produk yang sudah dibeli.",
                "Berapa ongkos kirim ke Surabaya?",
                "Kucing saya suka tidur di sofa.");
        List<float[]> vectors = client.embed(sentences);
        System.out.println("Dimensi vektor: " + vectors.get(0).length + "\n");

        for (int i = 1; i < sentences.size(); i++) {
            double sim = EmbeddingClient.cosine(vectors.get(0), vectors.get(i));
            System.out.printf("%.3f  \"%s\" vs \"%s\"%n", sim, sentences.get(0), sentences.get(i));
        }
    }
}
