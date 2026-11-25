package lab;

import java.util.List;

/** Week 4: ingest the policy document into pgvector, then full RAG with citations. */
public class Step8Rag {

    static final String SOURCE = "store-policy.md";

    static final String DOCUMENT = """
            Return Policy. Customers may request a return no later than 7 calendar days after receiving the item. \
            Returned items must be complete, with their original packaging and labels. \
            Returns are requested from the My Orders menu with an unboxing photo or video attached.

            Defective or Wrong Items. If an item arrives defective, damaged, or not as ordered, \
            the return shipping cost is fully covered by the store. \
            The customer can choose a replacement of the same item or a full refund.

            Change of Mind Returns. Returns because the customer changed their mind are accepted within 7 days, \
            but the return shipping cost is paid by the customer. \
            Underwear, opened cosmetics, and digital vouchers cannot be returned for a change of mind.

            Refunds. Refunds are issued no later than 5 business days after the returned item is received and inspected by the warehouse. \
            Refunds go to the original payment method or to the account balance, as the customer chooses.

            Shipping. Orders paid before 14:00 WIB ship the same day, except on Sundays and national holidays. \
            Shipping cost is based on the destination city and package weight, rounded up per kilogram. \
            Free shipping applies to orders above Rp500,000 to Java island.

            Warranty. Electronic products carry an official 12-month warranty from the purchase date. \
            Warranty claims do not cover damage caused by water, drops, or third-party repairs. \
            Warranty claims require the order number and the purchase receipt.

            Payment. The store accepts bank transfer, credit card, e-wallet, and cash on delivery (COD). \
            COD is only available for orders worth at most Rp2,000,000. \
            Orders not paid within 24 hours are cancelled automatically.

            Customer Service. The customer service team is available Monday to Saturday from 08:00 to 20:00 WIB via chat and email.
            """;

    /** Reset the table and store all document chunks. Returns the chunk count. Also used by the evals and the agent. */
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
            System.out.println("Ingest done: " + count + " chunks\n");

            RagService rag = new RagService(llm, embeddings, store);
            List<String> questions = List.of(
                    "How long is the return window?",
                    "If the item is defective, who pays the return shipping?",
                    "Can I pay COD for an order worth 3 million?",
                    "Does the store sell plane tickets?");
            for (String q : questions) {
                RagService.Answer answer = rag.answer(q);
                System.out.println("Question: " + q);
                for (DocStore.Hit hit : answer.context()) {
                    System.out.printf("  context (%.3f): %s%n", hit.similarity(), hit.content());
                }
                System.out.println("Answer: " + answer.text() + "\n");
            }
        }
    }
}
