package lab;

import com.fasterxml.jackson.databind.JsonNode;
import lab.LlmClient.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Week 6: agent. One assistant that combines the order tools (Week 2) with RAG (Week 4).
 * The model decides when to search the policy and when to call the order API.
 */
public class Step11Agent {

    static final String NO_POLICY = "NO_RESULTS: no relevant policy found for this query.";

    static final Map<String, Object> SEARCH_POLICY_TOOL = LlmClient.tool(EvalSet.SEARCH_POLICY,
            "Search the store policy document (returns, return shipping, refunds, shipping, "
                    + "warranty, payment, service hours). Results are numbered [n] for citations.",
            Map.of("query", LlmClient.param("string", "Keywords or a question about the policy")),
            List.of("query"));

    static final String SYSTEM_PROMPT = """
            You are a customer service assistant for an online store. Answer briefly.
            Rules:
            - Questions about store rules (returns, warranty, payment, shipping, refunds): call search_policy.
            - Questions about a specific order (a number like A-1001): call get_order_status.
            - Questions about the shipping fee to a specific city: call get_shipping_fee.
            - If a question needs both, call every tool it needs.
            - Never guess order data, prices, or policy content. Use only tool results.
            - Claims from the policy must carry a [n] citation matching the numbers in the search_policy result.
            - If search_policy returns NO_RESULTS, say the information is not in the policy.
            - If the question is outside the store's services, politely decline without calling any tool.""";

    static List<Map<String, Object>> tools() {
        List<Map<String, Object>> tools = new ArrayList<>(Step5ToolCalling.ORDER_TOOLS);
        tools.add(SEARCH_POLICY_TOOL);
        return tools;
    }

    /** The search_policy tool: chunks that pass MIN_SIMILARITY, numbered [n]. */
    static String searchPolicy(EmbeddingClient embeddings, DocStore store, String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query is required");
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
                throw new IllegalArgumentException("unknown tool '" + name + "'");
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
            System.out.println("Policy ingested: " + count + " chunks\n");

            ToolLoop loop = new ToolLoop(new LlmClient());
            ToolLoop.ToolExecutor executor = executor(embeddings, store);
            List<String> questions = List.of(
                    // policy only
                    "How long is the warranty on electronics?",
                    // order data only
                    "What is the status of order A-1002?",
                    // both
                    "Order A-1001 has shipped. If the item is defective, can I return it and who pays the shipping?",
                    // out of scope
                    "Who was the first president of Indonesia?");

            for (String q : questions) {
                System.out.println("Question: " + q);
                ToolLoop.Result result = ask(loop, executor, q);
                System.out.println("Tools used: " + (result.toolsCalled().isEmpty() ? "(none)" : result.toolsCalled()));
                System.out.println("Answer: " + result.answer() + "\n");
            }
        }
    }
}
