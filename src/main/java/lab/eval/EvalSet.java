package lab.eval;

import java.util.List;
import java.util.Set;

/** Evaluation data. Used by Step9RetrievalEval, Step10JudgeEval, and Step11AgentEval. */
public class EvalSet {

    /** expected = text snippet that must appear in the correct chunk; null = not answerable from the document. */
    public record Case(String question, String expected) {
        public boolean answerable() {
            return expected != null;
        }
    }

    public static final List<Case> CASES = List.of(
            new Case("How many days do I have to request a return?", "no later than 7 calendar days"),
            new Case("Who covers the shipping cost if the item is damaged?", "fully covered by the store"),
            new Case("If I change my mind, who pays the return shipping?", "paid by the customer"),
            new Case("Can opened cosmetics be returned?", "opened cosmetics"),
            new Case("How long until I get my money back after a return?", "no later than 5 business days"),
            new Case("What time do I need to pay for same-day shipping?", "before 14:00 WIB"),
            new Case("When do I get free shipping?", "above Rp500,000"),
            new Case("How long is the warranty on electronics?", "official 12-month warranty"),
            new Case("Does the warranty cover a phone damaged by water?", "damage caused by water"),
            new Case("Which payment methods are accepted?", "e-wallet"),
            new Case("What is the maximum order value for COD?", "at most Rp2,000,000"),
            new Case("What are the customer service hours?", "from 08:00 to 20:00 WIB"),
            new Case("Does the store have an offline branch in Bandung?", null),
            new Case("Can I pay in 0% installments with a credit card?", null),
            new Case("Who owns this store?", null));

    public static final String SEARCH_POLICY = "search_policy";
    public static final String ORDER_STATUS = "get_order_status";
    public static final String SHIPPING_FEE = "get_shipping_fee";

    /** Agent case: the tools that should be called. Empty set = call no tool. */
    public record AgentCase(String question, Set<String> expectedTools) {
    }

    public static final List<AgentCase> AGENT_CASES = List.of(
            new AgentCase("How many days is the return window?", Set.of(SEARCH_POLICY)),
            new AgentCase("Does the warranty cover a laptop damaged by water?", Set.of(SEARCH_POLICY)),
            new AgentCase("What is the status of order A-1002?", Set.of(ORDER_STATUS)),
            new AgentCase("What is the shipping fee to Surabaya for a 3 kg package?", Set.of(SHIPPING_FEE)),
            new AgentCase("Order A-1001 has shipped. If the item is defective, can I return it and who pays the shipping?",
                    Set.of(ORDER_STATUS, SEARCH_POLICY)),
            new AgentCase("Order A-1003 has arrived. If I change my mind, can I still return it?",
                    Set.of(ORDER_STATUS, SEARCH_POLICY)),
            new AgentCase("Who was the first president of Indonesia?", Set.of()),
            new AgentCase("Please write me a poem about rain.", Set.of()));
}
