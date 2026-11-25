package lab;

import com.fasterxml.jackson.databind.JsonNode;
import lab.LlmClient.Message;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minggu 2: tool calling. Model memilih tool, data pesanan dan hitungan ongkir datang dari kode Java.
 */
public class Step5ToolCalling {

    record Order(String id, String status, String courier, String detail) {
    }

    /** Data tiruan pengganti API pesanan. */
    static final Map<String, Order> ORDERS = Map.of(
            "A-1001", new Order("A-1001", "DIKIRIM", "JNE", "resi JNE123456, estimasi tiba 2026-11-27"),
            "A-1002", new Order("A-1002", "DIPROSES", "-", "sedang dikemas di gudang"),
            "A-1003", new Order("A-1003", "SELESAI", "SiCepat", "diterima pelanggan 2026-11-20"));

    /** Tarif per kilogram (rupiah). Berat dibulatkan ke atas per kg. */
    static final Map<String, Integer> RATE_PER_KG = Map.of(
            "jakarta", 10_000,
            "bandung", 12_000,
            "surabaya", 15_000,
            "medan", 25_000,
            "makassar", 28_000);

    static final List<Map<String, Object>> ORDER_TOOLS = List.of(
            LlmClient.tool(EvalSet.ORDER_STATUS,
                    "Cek status dan detail pengiriman pesanan berdasarkan nomor pesanan.",
                    Map.of("order_id", LlmClient.param("string", "Nomor pesanan, format A-0000")),
                    List.of("order_id")),
            LlmClient.tool(EvalSet.SHIPPING_FEE,
                    "Hitung ongkos kirim ke kota tujuan berdasarkan berat paket.",
                    Map.of(
                            "city", LlmClient.param("string", "Kota tujuan"),
                            "weight_kg", LlmClient.param("number", "Berat paket dalam kilogram")),
                    List.of("city", "weight_kg")));

    /** Menjalankan tool pesanan. Mengembalikan null jika nama tool bukan tool pesanan. */
    static String executeOrderTool(String name, JsonNode args) {
        return switch (name) {
            case EvalSet.ORDER_STATUS -> getOrderStatus(args.path("order_id").asText(""));
            case EvalSet.SHIPPING_FEE -> getShippingFee(args.path("city").asText(""), args.path("weight_kg"));
            default -> null;
        };
    }

    static String getOrderStatus(String orderId) {
        Order order = ORDERS.get(orderId.strip().toUpperCase(Locale.ROOT));
        if (order == null) {
            throw new IllegalArgumentException("pesanan '" + orderId + "' tidak ditemukan");
        }
        return "Pesanan %s: status %s, kurir %s, %s".formatted(order.id(), order.status(), order.courier(), order.detail());
    }

    static String getShippingFee(String city, JsonNode weight) {
        Integer rate = RATE_PER_KG.get(city.strip().toLowerCase(Locale.ROOT));
        if (rate == null) {
            throw new IllegalArgumentException("kota '" + city + "' belum dilayani. Kota tersedia: " + RATE_PER_KG.keySet());
        }
        double kg = weight.isNumber() ? weight.asDouble() : parseNumber(weight.asText(""));
        if (kg <= 0 || kg > 50) {
            throw new IllegalArgumentException("weight_kg harus antara 0 dan 50");
        }
        long billedKg = (long) Math.ceil(kg);
        long fee = billedKg * rate;
        return "Ongkir ke %s untuk %.1f kg (dihitung %d kg): Rp%,d".formatted(city, kg, billedKg, fee)
                .replace(',', '.');
    }

    private static double parseNumber(String text) {
        try {
            return Double.parseDouble(text.replace(",", ".").replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("weight_kg bukan angka: '" + text + "'");
        }
    }

    public static void main(String[] args) {
        ToolLoop loop = new ToolLoop(new LlmClient());
        Message system = Message.system("""
                Kamu asisten toko online. Gunakan tool untuk data pesanan dan ongkir, jangan menebak.
                Jawab singkat dalam bahasa Indonesia.""");

        List<String> questions = List.of(
                "Pesanan A-1001 sudah sampai mana?",
                "Berapa ongkir ke Medan untuk paket 2,5 kg?",
                "Cek pesanan A-1002, dan berapa ongkir ke Bandung untuk 1 kg?");
        for (String q : questions) {
            System.out.println("Pertanyaan: " + q);
            ToolLoop.Result result = loop.run(List.of(system, Message.user(q)), ORDER_TOOLS, (name, a) -> {
                String output = executeOrderTool(name, a);
                if (output == null) {
                    throw new IllegalArgumentException("tool '" + name + "' tidak dikenal");
                }
                return output;
            });
            System.out.println("Jawaban: " + result.answer() + "\n");
        }
    }
}
