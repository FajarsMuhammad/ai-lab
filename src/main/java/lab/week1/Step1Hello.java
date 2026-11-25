package lab.week1;

import lab.llm.LlmClient;
import lab.llm.LlmClient.Message;

import java.util.List;

/** Week 1: the first chat call. */
public class Step1Hello {

    public static void main(String[] args) {
        LlmClient llm = new LlmClient();
        String reply = llm.chat(List.of(
                Message.system("You are an assistant that answers briefly."),
                Message.user("Explain what an LLM is in two sentences.")));
        System.out.println("Model: " + reply);
    }
}
