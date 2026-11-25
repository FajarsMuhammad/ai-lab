package lab;

import com.fasterxml.jackson.databind.JsonNode;
import lab.LlmClient.Message;
import lab.LlmClient.ToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loop tool calling: model meminta tool, kode Java menjalankannya, hasilnya dikirim balik,
 * sampai model memberi jawaban akhir atau batas MAX_STEPS tercapai.
 * Dipakai Step5ToolCalling dan Step11Agent.
 */
public class ToolLoop {

    public static final int MAX_STEPS = 5;
    public static final String GAVE_UP = "Maaf, saya belum bisa menyelesaikan permintaan ini.";

    /** Menjalankan satu tool. Argumen sudah di-parse; nilai di dalamnya tetap harus divalidasi. */
    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String name, JsonNode arguments);
    }

    /** toolsCalled = nama tool sesuai urutan pemanggilan, dipakai evaluasi agent. */
    public record Result(String answer, List<String> toolsCalled, int steps) {
    }

    private final LlmClient llm;
    private final boolean verbose;

    public ToolLoop(LlmClient llm) {
        this(llm, true);
    }

    public ToolLoop(LlmClient llm, boolean verbose) {
        this.llm = llm;
        this.verbose = verbose;
    }

    public Result run(List<Message> initialMessages, List<Map<String, Object>> tools, ToolExecutor executor) {
        List<Message> messages = new ArrayList<>(initialMessages);
        List<String> toolsCalled = new ArrayList<>();

        for (int step = 1; step <= MAX_STEPS; step++) {
            Message reply = llm.chatWithTools(messages, tools);
            if (!reply.hasToolCalls()) {
                return new Result(reply.content(), toolsCalled, step);
            }
            messages.add(reply);
            for (ToolCall call : reply.toolCalls()) {
                String name = call.function().name();
                String rawArgs = call.function().arguments();
                toolsCalled.add(name);
                String output = executeSafely(executor, name, rawArgs);
                if (verbose) {
                    System.out.println("  [langkah " + step + "] " + name + "(" + rawArgs + ")");
                    System.out.println("      -> " + output.replace("\n", "\n         "));
                }
                messages.add(Message.tool(call.id(), output));
            }
        }
        if (verbose) {
            System.out.println("  Batas " + MAX_STEPS + " langkah tercapai, berhenti.");
        }
        return new Result(GAVE_UP, toolsCalled, MAX_STEPS);
    }

    /** Argumen dari model adalah input eksternal: JSON rusak atau error dikembalikan ke model sebagai pesan. */
    private static String executeSafely(ToolExecutor executor, String name, String rawArgs) {
        JsonNode args;
        try {
            args = LlmClient.MAPPER.readTree(rawArgs == null || rawArgs.isBlank() ? "{}" : rawArgs);
        } catch (Exception e) {
            return "ERROR: argumen bukan JSON valid";
        }
        try {
            return executor.execute(name, args);
        } catch (IllegalArgumentException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
