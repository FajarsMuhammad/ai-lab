package lab;

import java.util.ArrayList;
import java.util.List;

/** Memecah teks per kalimat sampai maxChars, dengan overlap 1 kalimat antar chunk. */
public class Chunker {

    private final int maxChars;

    public Chunker(int maxChars) {
        this.maxChars = maxChars;
    }

    public List<String> split(String text) {
        String[] sentences = text.trim().split("(?<=[.!?])\\s+|\\n\\s*\\n");
        List<String> chunks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int length = 0;

        for (String raw : sentences) {
            String sentence = raw.strip();
            if (sentence.isEmpty()) {
                continue;
            }
            if (!current.isEmpty() && length + sentence.length() + 1 > maxChars) {
                chunks.add(String.join(" ", current));
                // overlap: kalimat terakhir ikut ke chunk berikutnya
                String last = current.get(current.size() - 1);
                current = new ArrayList<>(List.of(last));
                length = last.length();
            }
            current.add(sentence);
            length += sentence.length() + 1;
        }
        if (!current.isEmpty()) {
            chunks.add(String.join(" ", current));
        }
        return chunks;
    }
}
