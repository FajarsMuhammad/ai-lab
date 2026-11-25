package lab;

import java.util.List;
import java.util.Set;

/** Data evaluasi. Dipakai Step9RetrievalEval, Step10JudgeEval, dan Step11AgentEval. */
public class EvalSet {

    /** expected = potongan teks yang wajib ada di chunk benar; null = tidak bisa dijawab dari dokumen. */
    public record Case(String question, String expected) {
        public boolean answerable() {
            return expected != null;
        }
    }

    public static final List<Case> CASES = List.of(
            new Case("Berapa hari batas waktu mengajukan retur?", "paling lambat 7 hari kalender"),
            new Case("Siapa yang menanggung ongkir kalau barangnya rusak?", "ditanggung sepenuhnya oleh toko"),
            new Case("Kalau saya berubah pikiran, ongkir returnya siapa yang bayar?", "ditanggung oleh pelanggan"),
            new Case("Apakah kosmetik yang sudah dibuka bisa dikembalikan?", "kosmetik yang sudah dibuka"),
            new Case("Berapa lama uang saya dikembalikan setelah retur?", "paling lambat 5 hari kerja"),
            new Case("Jam berapa batas pembayaran supaya dikirim hari ini?", "sebelum pukul 14.00 WIB"),
            new Case("Kapan dapat gratis ongkir?", "di atas Rp500.000"),
            new Case("Berapa lama garansi produk elektronik?", "garansi resmi 12 bulan"),
            new Case("Apakah garansi berlaku kalau HP kena air?", "kerusakan akibat air"),
            new Case("Metode pembayaran apa saja yang diterima?", "dompet digital"),
            new Case("Berapa batas maksimal nilai pesanan untuk COD?", "maksimal Rp2.000.000"),
            new Case("Jam berapa customer service buka?", "pukul 08.00 sampai 20.00 WIB"),
            new Case("Apakah toko punya cabang offline di Bandung?", null),
            new Case("Bisa cicilan 0% pakai kartu kredit?", null),
            new Case("Siapa pemilik toko ini?", null));

    public static final String SEARCH_POLICY = "search_policy";
    public static final String ORDER_STATUS = "get_order_status";
    public static final String SHIPPING_FEE = "get_shipping_fee";

    /** Kasus agent: tool yang seharusnya dipanggil. Set kosong = jangan panggil tool apa pun. */
    public record AgentCase(String question, Set<String> expectedTools) {
    }

    public static final List<AgentCase> AGENT_CASES = List.of(
            new AgentCase("Berapa hari batas waktu retur barang?", Set.of(SEARCH_POLICY)),
            new AgentCase("Apakah garansi berlaku kalau laptop kena air?", Set.of(SEARCH_POLICY)),
            new AgentCase("Bagaimana status pesanan A-1002?", Set.of(ORDER_STATUS)),
            new AgentCase("Berapa ongkir ke Surabaya untuk paket 3 kg?", Set.of(SHIPPING_FEE)),
            new AgentCase("Pesanan A-1001 sudah dikirim, kalau barangnya cacat bisa retur dan siapa yang bayar ongkirnya?",
                    Set.of(ORDER_STATUS, SEARCH_POLICY)),
            new AgentCase("Pesanan A-1003 sudah sampai, kalau saya berubah pikiran masih bisa retur?",
                    Set.of(ORDER_STATUS, SEARCH_POLICY)),
            new AgentCase("Siapa presiden pertama Indonesia?", Set.of()),
            new AgentCase("Tolong buatkan puisi tentang hujan.", Set.of()));
}
