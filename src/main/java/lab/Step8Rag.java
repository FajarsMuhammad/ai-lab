package lab;

import java.util.List;

/** Minggu 4: ingest dokumen kebijakan ke pgvector, lalu RAG lengkap dengan sitasi. */
public class Step8Rag {

    static final String SOURCE = "kebijakan-toko.md";

    static final String DOCUMENT = """
            Kebijakan Retur. Pelanggan dapat mengajukan retur paling lambat 7 hari kalender setelah barang diterima. \
            Barang yang diretur harus dalam kondisi lengkap beserta kemasan dan label aslinya. \
            Retur diajukan melalui menu Pesanan Saya dengan melampirkan foto atau video unboxing.

            Barang Cacat atau Salah Kirim. Jika barang diterima dalam keadaan cacat, rusak, atau tidak sesuai pesanan, \
            ongkos kirim retur ditanggung sepenuhnya oleh toko. \
            Pelanggan dapat memilih penggantian barang yang sama atau pengembalian dana penuh.

            Retur Karena Berubah Pikiran. Retur karena pelanggan berubah pikiran tetap diterima dalam 7 hari, \
            tetapi ongkos kirim retur ditanggung oleh pelanggan. \
            Produk pakaian dalam, kosmetik yang sudah dibuka, dan voucher digital tidak dapat diretur karena berubah pikiran.

            Pengembalian Dana. Dana dikembalikan paling lambat 5 hari kerja setelah barang retur diterima dan diperiksa gudang. \
            Pengembalian dana dikirim ke metode pembayaran awal atau ke saldo akun sesuai pilihan pelanggan.

            Pengiriman. Pesanan yang dibayar sebelum pukul 14.00 WIB dikirim di hari yang sama, kecuali hari Minggu dan hari libur nasional. \
            Ongkos kirim dihitung dari kota tujuan dan berat paket, dengan pembulatan ke atas per kilogram. \
            Gratis ongkir berlaku untuk pesanan di atas Rp500.000 ke Pulau Jawa.

            Garansi. Produk elektronik bergaransi resmi 12 bulan sejak tanggal pembelian. \
            Klaim garansi tidak berlaku untuk kerusakan akibat air, jatuh, atau perbaikan oleh pihak ketiga. \
            Klaim garansi diajukan dengan menyertakan nomor pesanan dan nota pembelian.

            Pembayaran. Toko menerima transfer bank, kartu kredit, dompet digital, dan bayar di tempat (COD). \
            COD hanya tersedia untuk pesanan dengan nilai maksimal Rp2.000.000. \
            Pesanan yang belum dibayar dalam 24 jam akan dibatalkan otomatis.

            Layanan Pelanggan. Tim layanan pelanggan aktif Senin sampai Sabtu pukul 08.00 sampai 20.00 WIB melalui chat dan email.
            """;

    /** Reset tabel lalu simpan semua chunk dokumen. Mengembalikan jumlah chunk. Dipakai juga oleh eval dan agent. */
    static int ingest(DocStore store, EmbeddingClient embeddings, int chunkSize) {
        List<String> chunks = new Chunker(chunkSize).split(DOCUMENT);
        List<float[]> vectors = embeddings.embed(chunks);
        store.reset(vectors.get(0).length);
        for (int i = 0; i < chunks.size(); i++) {
            store.add(SOURCE, chunks.get(i), vectors.get(i));
        }
        return chunks.size();
    }

    public static void main(String[] args) {
        LlmClient llm = new LlmClient();
        EmbeddingClient embeddings = new EmbeddingClient();
        try (DocStore store = new DocStore()) {
            int count = ingest(store, embeddings, 400);
            System.out.println("Ingest selesai: " + count + " chunk\n");

            RagService rag = new RagService(llm, embeddings, store);
            List<String> questions = List.of(
                    "Berapa lama batas waktu retur?",
                    "Kalau barang cacat, siapa yang bayar ongkir retur?",
                    "Apakah bisa bayar COD untuk pesanan 3 juta?",
                    "Apakah toko menjual tiket pesawat?");
            for (String q : questions) {
                RagService.Answer answer = rag.answer(q);
                System.out.println("Pertanyaan: " + q);
                for (DocStore.Hit hit : answer.context()) {
                    System.out.printf("  konteks (%.3f): %s%n", hit.similarity(), hit.content());
                }
                System.out.println("Jawaban: " + answer.text() + "\n");
            }
        }
    }
}
