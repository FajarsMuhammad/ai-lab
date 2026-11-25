package lab;

import com.fasterxml.jackson.databind.JsonNode;
import lab.LlmClient.Message;
import lab.LlmClient.ToolCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tool calling loop: the model requests a tool, Java code runs it, the result is sent back,
 * until the model gives a final answer or MAX_STEPS is reached.
 * Used by Step5ToolCalling and Step11Agent.
 */
public class ToolLoop {

    public static final int MAX_STEPS = 5;
    public static final String GAVE_UP = "Sorry, I could not complete this request.";

    /** Runs one tool. Arguments are already parsed; their values must still be validated. */
    @FunctionalInterface
    public interface ToolExecutor {
        String execute(String name, JsonNode arguments);
    }

    /** toolsCalled = tool names in call order, used by the agent eval. */
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
                    System.out.println("  [step " + step + "] " + name + "(" + rawArgs + ")");
                    System.out.println("      -> " + output.replace("\n", "\n         "));
                }
                messages.add(Message.tool(call.id(), output));
            }
        }
        if (verbose) {
            System.out.println("  Reached the limit of " + MAX_STEPS + " steps, stopping.");
        }
        return new Result(GAVE_UP, toolsCalled, MAX_STEPS);
    }

    /** Model arguments are external input: broken JSON or errors are returned to the model as a message. */
    private static String executeSafely(ToolExecutor executor, String name, String rawArgs) {
        JsonNode args;
        try {
            args = LlmClient.MAPPER.readTree(rawArgs == null || rawArgs.isBlank() ? "{}" : rawArgs);
        } catch (Exception e) {
            return "ERROR: arguments are not valid JSON";
        }
        try {
            return executor.execute(name, args);
        } catch (IllegalArgumentException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
