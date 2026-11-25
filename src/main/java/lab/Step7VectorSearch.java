package lab;

import java.util.List;

/** Minggu 3: simpan embedding di pgvector lalu cari dengan jarak cosine. */
public class Step7VectorSearch {

    public static void main(String[] args) {
        EmbeddingClient embeddings = new EmbeddingClient();
        List<String> faqs = List.of(
                "Retur dapat diajukan maksimal 7 hari setelah barang diterima.",
                "Pengiriman dilakukan setiap hari kecuali Minggu.",
                "Produk elektronik bergaransi 12 bulan.",
                "Kami menerima pembayaran COD sampai Rp2.000.000.");

        try (DocStore store = new DocStore()) {
            List<float[]> vectors = embeddings.embed(faqs);
            store.reset(vectors.get(0).length);
            for (int i = 0; i < faqs.size(); i++) {
                store.add("faq", faqs.get(i), vectors.get(i));
            }

            String query = "Berapa lama garansi laptop?";
            System.out.println("Query: " + query);
            for (DocStore.Hit hit : store.search(embeddings.embed(query), 3)) {
                System.out.printf("  %.3f  %s%n", hit.similarity(), hit.content());
            }
        }
    }
}
