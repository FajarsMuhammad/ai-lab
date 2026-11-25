package lab.week2;

import com.fasterxml.jackson.databind.JsonNode;
import lab.eval.EvalSet;
import lab.llm.LlmClient;
import lab.llm.LlmClient.Message;
import lab.llm.ToolLoop;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Week 2: tool calling. The model picks the tool; order data and shipping math come from Java code.
 */
public class Step5ToolCalling {

    record Order(String id, String status, String courier, String detail) {
    }

    /** Fake data standing in for the order API. */
    static final Map<String, Order> ORDERS = Map.of(
            "A-1001", new Order("A-1001", "SHIPPED", "JNE", "tracking JNE123456, estimated arrival 2026-11-27"),
            "A-1002", new Order("A-1002", "PROCESSING", "-", "being packed at the warehouse"),
            "A-1003", new Order("A-1003", "COMPLETED", "SiCepat", "received by customer 2026-11-20"));

    /** Rate per kilogram (rupiah). Weight is rounded up per kg. */
    static final Map<String, Integer> RATE_PER_KG = Map.of(
            "jakarta", 10_000,
            "bandung", 12_000,
            "surabaya", 15_000,
            "medan", 25_000,
            "makassar", 28_000);

    public static final List<Map<String, Object>> ORDER_TOOLS = List.of(
            LlmClient.tool(EvalSet.ORDER_STATUS,
                    "Check the status and shipping details of an order by order number.",
                    Map.of("order_id", LlmClient.param("string", "Order number, format A-0000")),
                    List.of("order_id")),
            LlmClient.tool(EvalSet.SHIPPING_FEE,
                    "Calculate the shipping fee to a destination city based on package weight.",
                    Map.of(
                            "city", LlmClient.param("string", "Destination city"),
                            "weight_kg", LlmClient.param("number", "Package weight in kilograms")),
                    List.of("city", "weight_kg")));

    /** Runs an order tool. Returns null if the name is not an order tool. */
    public static String executeOrderTool(String name, JsonNode args) {
        return switch (name) {
            case EvalSet.ORDER_STATUS -> getOrderStatus(args.path("order_id").asText(""));
            case EvalSet.SHIPPING_FEE -> getShippingFee(args.path("city").asText(""), args.path("weight_kg"));
            default -> null;
        };
    }

    static String getOrderStatus(String orderId) {
        Order order = ORDERS.get(orderId.strip().toUpperCase(Locale.ROOT));
        if (order == null) {
            throw new IllegalArgumentException("order '" + orderId + "' not found");
        }
        return "Order %s: status %s, courier %s, %s".formatted(order.id(), order.status(), order.courier(), order.detail());
    }

    static String getShippingFee(String city, JsonNode weight) {
        Integer rate = RATE_PER_KG.get(city.strip().toLowerCase(Locale.ROOT));
        if (rate == null) {
            throw new IllegalArgumentException("city '" + city + "' is not served yet. Available cities: " + RATE_PER_KG.keySet());
        }
        double kg = weight.isNumber() ? weight.asDouble() : parseNumber(weight.asText(""));
        if (kg <= 0 || kg > 50) {
            throw new IllegalArgumentException("weight_kg must be between 0 and 50");
        }
        long billedKg = (long) Math.ceil(kg);
        long fee = billedKg * rate;
        return String.format(Locale.ROOT, "Shipping fee to %s for %.1f kg (billed %d kg): Rp%,d", city, kg, billedKg, fee);
    }

    private static double parseNumber(String text) {
        try {
            return Double.parseDouble(text.replace(",", ".").replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("weight_kg is not a number: '" + text + "'");
        }
    }

    public static void main(String[] args) {
        ToolLoop loop = new ToolLoop(new LlmClient());
        Message system = Message.system("""
                You are an online store assistant. Use the tools for order data and shipping fees, never guess.
                Answer briefly.""");

        List<String> questions = List.of(
                "Where is order A-1001 now?",
                "What is the shipping fee to Medan for a 2.5 kg package?",
                "Check order A-1002, and what is the shipping fee to Bandung for 1 kg?");
        for (String q : questions) {
            System.out.println("Question: " + q);
            ToolLoop.Result result = loop.run(List.of(system, Message.user(q)), ORDER_TOOLS, (name, a) -> {
                String output = executeOrderTool(name, a);
                if (output == null) {
                    throw new IllegalArgumentException("unknown tool '" + name + "'");
                }
                return output;
            });
            System.out.println("Answer: " + result.answer() + "\n");
        }
    }
}
