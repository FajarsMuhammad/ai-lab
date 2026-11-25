package lab;

import com.fasterxml.jackson.databind.JsonNode;
import lab.LlmClient.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Minggu 6: agent. Satu asisten yang menggabungkan tool pesanan (Minggu 2) dan RAG (Minggu 4).
 * Model yang memutuskan kapan mencari kebijakan dan kapan memanggil API pesanan.
 */
public class Step11Agent {

    static final String NO_POLICY = "TIDAK_ADA_HASIL: tidak ada kebijakan yang relevan untuk query ini.";

    static final Map<String, Object> SEARCH_POLICY_TOOL = LlmClient.tool(EvalSet.SEARCH_POLICY,
            "Cari potongan dokumen kebijakan toko (retur, ongkir retur, pengembalian dana, pengiriman, "
                    + "garansi, pembayaran, jam layanan). Hasil bernomor [n] untuk sitasi.",
            Map.of("query", LlmClient.param("string", "Kata kunci atau pertanyaan tentang kebijakan")),
            List.of("query"));

    static final String SYSTEM_PROMPT = """
            Kamu asisten layanan pelanggan toko online. Jawab singkat dalam bahasa Indonesia.
            Aturan:
            - Pertanyaan tentang aturan toko (retur, garansi, pembayaran, pengiriman, pengembalian dana): panggil search_policy.
            - Pertanyaan tentang pesanan tertentu (nomor seperti A-1001): panggil get_order_status.
            - Pertanyaan tentang biaya kirim ke kota tertentu: panggil get_shipping_fee.
            - Jika pertanyaan butuh keduanya, panggil semua tool yang diperlukan.
            - Jangan menebak data pesanan, harga, atau isi kebijakan. Pakai hanya hasil tool.
            - Klaim dari kebijakan wajib diberi sitasi [n] sesuai nomor di hasil search_policy.
            - Jika search_policy mengembalikan TIDAK_ADA_HASIL, katakan bahwa informasinya tidak ada di kebijakan.
            - Jika pertanyaan di luar layanan toko, tolak dengan sopan tanpa memanggil tool.""";

    static List<Map<String, Object>> tools() {
        List<Map<String, Object>> tools = new ArrayList<>(Step5ToolCalling.ORDER_TOOLS);
        tools.add(SEARCH_POLICY_TOOL);
        return tools;
    }

    /** Tool search_policy: chunk yang lolos MIN_SIMILARITY, bernomor [n]. */
    static String searchPolicy(EmbeddingClient embeddings, DocStore store, String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query wajib diisi");
        }
        List<DocStore.Hit> hits = store.search(embeddings.embed(query), RagService.TOP_K).stream()
                .filter(hit -> hit.similarity() >= RagService.MIN_SIMILARITY)
                .toList();
        return hits.isEmpty() ? NO_POLICY : RagService.formatContext(hits);
    }

    static ToolLoop.ToolExecutor executor(EmbeddingClient embeddings, DocStore store) {
        return (String name, JsonNode args) -> {
            if (EvalSet.SEARCH_POLICY.equals(name)) {
                return searchPolicy(embeddings, store, args.path("query").asText(""));
            }
            String output = Step5ToolCalling.executeOrderTool(name, args);
            if (output == null) {
                throw new IllegalArgumentException("tool '" + name + "' tidak dikenal");
            }
            return output;
        };
    }

    static ToolLoop.Result ask(ToolLoop loop, ToolLoop.ToolExecutor executor, String question) {
        return loop.run(List.of(Message.system(SYSTEM_PROMPT), Message.user(question)), tools(), executor);
    }

    public static void main(String[] args) {
        EmbeddingClient embeddings = new EmbeddingClient();
        try (DocStore store = new DocStore()) {
            int count = Step8Rag.ingest(store, embeddings, 400);
            System.out.println("Ingest kebijakan: " + count + " chunk\n");

            ToolLoop loop = new ToolLoop(new LlmClient());
            ToolLoop.ToolExecutor executor = executor(embeddings, store);
            List<String> questions = List.of(
                    // hanya kebijakan
                    "Berapa lama garansi produk elektronik?",
                    // hanya data pesanan
                    "Bagaimana status pesanan A-1002?",
                    // keduanya
                    "Pesanan A-1001 sudah dikirim, kalau barangnya cacat bisa retur dan siapa yang bayar ongkirnya?",
                    // di luar cakupan
                    "Siapa presiden pertama Indonesia?");

            for (String q : questions) {
                System.out.println("Pertanyaan: " + q);
                ToolLoop.Result result = ask(loop, executor, q);
                System.out.println("Tool dipakai: " + (result.toolsCalled().isEmpty() ? "(tidak ada)" : result.toolsCalled()));
                System.out.println("Jawaban: " + result.answer() + "\n");
            }
        }
    }
}
