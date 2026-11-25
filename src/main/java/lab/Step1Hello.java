package lab;

import lab.LlmClient.Message;

import java.util.List;

/** Minggu 1: panggilan chat pertama. */
public class Step1Hello {

    public static void main(String[] args) {
        LlmClient llm = new LlmClient();
        String reply = llm.chat(List.of(
                Message.system("Kamu asisten yang menjawab singkat dalam bahasa Indonesia."),
                Message.user("Jelaskan apa itu LLM dalam dua kalimat.")));
        System.out.println("Model: " + reply);
    }
}
