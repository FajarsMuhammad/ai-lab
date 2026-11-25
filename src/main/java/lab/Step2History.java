package lab;

import lab.LlmClient.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Minggu 1: LLM tidak punya memori. Riwayat percakapan harus dikirim ulang setiap kali.
 */
public class Step2History {

    public static void main(String[] args) {
        LlmClient llm = new LlmClient();
        List<Message> history = new ArrayList<>();
        history.add(Message.system("Kamu asisten yang menjawab singkat dalam bahasa Indonesia."));

        List<String> turns = List.of(
                "Nama saya Budi dan saya suka kopi.",
                "Apa minuman favorit saya?",
                "Siapa nama saya?");
        for (String turn : turns) {
            history.add(Message.user(turn));
            String reply = llm.chat(history, 0.0);
            history.add(Message.assistant(reply));
            System.out.println("Pengguna: " + turn);
            System.out.println("Model   : " + reply + "\n");
        }

        // Tanpa riwayat, model tidak tahu nama pengguna
        String noHistory = llm.chat(List.of(Message.user("Siapa nama saya?")), 0.0);
        System.out.println("Tanpa riwayat: " + noHistory);
    }
}
