package lab;

import lab.LlmClient.Message;

import java.util.List;

/** Minggu 1: pengaruh temperature terhadap variasi jawaban. */
public class Step3Temperature {

    public static void main(String[] args) {
        LlmClient llm = new LlmClient();
        List<Message> prompt = List.of(Message.user("Berikan satu nama untuk toko kopi kecil. Jawab nama saja."));

        for (double temperature : new double[] {0.0, 1.0}) {
            System.out.println("temperature = " + temperature);
            for (int i = 1; i <= 3; i++) {
                System.out.println("  " + i + ". " + llm.chat(prompt, temperature).strip());
            }
        }
    }
}
