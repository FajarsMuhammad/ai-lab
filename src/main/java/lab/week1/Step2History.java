package lab.week1;

import lab.llm.LlmClient;
import lab.llm.LlmClient.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Week 1: an LLM has no memory. The conversation history must be sent again on every call.
 */
public class Step2History {

    public static void main(String[] args) {
        LlmClient llm = new LlmClient();
        List<Message> history = new ArrayList<>();
        history.add(Message.system("You are an assistant that answers briefly."));

        List<String> turns = List.of(
                "My name is Budi and I like coffee.",
                "What is my favorite drink?",
                "What is my name?");
        for (String turn : turns) {
            history.add(Message.user(turn));
            String reply = llm.chat(history, 0.0);
            history.add(Message.assistant(reply));
            System.out.println("User : " + turn);
            System.out.println("Model: " + reply + "\n");
        }

        // Without history, the model does not know the user's name
        String noHistory = llm.chat(List.of(Message.user("What is my name?")), 0.0);
        System.out.println("Without history: " + noHistory);
    }
}
