package lab.week1;

import lab.llm.LlmClient;
import lab.llm.LlmClient.Message;

import java.util.List;

/** Week 1: how temperature affects answer variety. */
public class Step3Temperature {

    public static void main(String[] args) {
        LlmClient llm = new LlmClient();
        List<Message> prompt = List.of(Message.user("Suggest one name for a small coffee shop. Reply with the name only."));

        for (double temperature : new double[] {0.0, 1.0}) {
            System.out.println("temperature = " + temperature);
            for (int i = 1; i <= 3; i++) {
                System.out.println("  " + i + ". " + llm.chat(prompt, temperature).strip());
            }
        }
    }
}
