# ai-lab: learning AI Engineering with Java

A step-by-step AI Engineer learning project. Everything runs **locally on a laptop** (no GPU, no paid APIs): models via Ollama, vector DB via pgvector in Docker.

## Stack

- Java 27+, Maven, no framework (only `java.net.http.HttpClient` and JDBC)
- Jackson (`jackson-databind`) for JSON
- PostgreSQL + pgvector (`org.postgresql:postgresql`)
- Ollama, with the OpenAI-compatible endpoint at `http://localhost:11434/v1`
  - chat: `llama3.2:3b`
  - embedding: `nomic-embed-text` (multilingual alternative: `bge-m3`)

## Setup

```bash
ollama pull llama3.2:3b
ollama pull nomic-embed-text
docker run -d --name pgvector -e POSTGRES_PASSWORD=postgres -p 5432:5432 pgvector/pgvector:pg16
mvn -q compile
mvn -q exec:java -Dexec.mainClass=lab.week1.Step1Hello   # needs exec-maven-plugin, or run main from the IDE
```

## Structure

All classes live under `src/main/java/lab/`, one package per folder:

```
lab/
├── llm/    LlmClient, EmbeddingClient, ToolLoop   (model clients)
├── rag/    Chunker, DocStore, RagService          (retrieval)
├── eval/   EvalSet                                (eval data)
├── week1/  Step1Hello ... Step4Structured
├── week2/  Step5ToolCalling
├── week3/  Step6EmbeddingBasics, Step7VectorSearch
├── week4/  Step8Rag
├── week5/  Step9RetrievalEval, Step10JudgeEval
└── week6/  Step11Agent, Step11AgentEval
```

Run a Step by its full name, for example `lab.week6.Step11Agent`.

| File | Role |
|---|---|
| `LlmClient` | Chat client: `chat`, `chatJson` (JSON mode), `chatWithTools` (tool calling). Records `Message`, `ToolCall`, `FunctionCall` |
| `EmbeddingClient` | `embed(String)` and `embed(List<String>)` (batch, order preserved) |
| `ToolLoop` | Shared tool calling loop (`MAX_STEPS = 5`), prints every tool call, returns `Result(answer, toolsCalled, steps)` |
| `Chunker` | Splits text by sentence up to `maxChars`, 1-sentence overlap |
| `DocStore` | `chunks` table in pgvector: `reset`, `add`, `search` (`<=>` operator, cosine distance) |
| `RagService` | Retrieve top-3, filter by `MIN_SIMILARITY`, prompt with `[n]` citations. Does not call the LLM when there is no context |
| `EvalSet` | 15 eval cases; `expected` = text snippet that must be in the correct chunk, `null` = not answerable. `AGENT_CASES`: 8 agent cases with expected tools |
| `Step1Hello` ... `Step4Structured` | Week 1: chat, history, temperature, structured output + validation + retry |
| `Step5ToolCalling` | Week 2: tool calling loop (`MAX_STEPS = 5`), tools `get_order_status`, `get_shipping_fee` |
| `Step6EmbeddingBasics`, `Step7VectorSearch` | Week 3: cosine similarity, vector search in pgvector |
| `Step8Rag` | Week 4: ingest the policy document + full RAG. `DOCUMENT`, `SOURCE`, and `ingest` are public, used by the evals and the agent |
| `Step9RetrievalEval` | Week 5: chunk size × threshold grid, metrics hit@1, hit@3, reject, false-reject, false-accept |
| `Step10JudgeEval` | Week 5: LLM-as-judge for grounded / hallucinated |
| `Step11Agent` | Week 6: agent with the order tools + `search_policy` (chunks passing `MIN_SIMILARITY`, numbered `[n]`) |
| `Step11AgentEval` | Week 6: eval of tool choice per question (correct, missed, unneeded) |

## Code conventions

- Simple and readable. Every `StepN` is a standalone program with `main` that can be copied and run directly.
- Do not add frameworks (Spring AI, LangChain4j) unless asked. The goal is to understand what frameworks abstract away.
- Always treat model output as external input: parse, validate, and retry with the error message.
- Calculations and data (prices, dates, statuses) come from Java code or tools, never from the model.
- Comments, console text, prompts, and sample data are in English.
- Shared classes go in `llm/`, `rag/`, or `eval/`; a new Step goes in the `weekN/` package of its week. Members used from another package must be `public`.
- Keep changes as small as possible. Do not refactor old Step files when adding a new Step; shared classes (`LlmClient`, `DocStore`, and others) may be extended as long as old Steps still run.

## Status

- [x] Week 1: LLM API basics
- [x] Week 2: tool calling
- [x] Week 3: embeddings + pgvector
- [x] Week 4: full RAG
- [x] Week 5: evaluation
- [x] Week 6: agent
- [ ] Week 7: observability, caching, cost control (next)
- [ ] Week 8: deploy the final project (Docker) + README

## Week 6, agent (done)

Build `Step11Agent`: one assistant that combines Week 2 and Week 4.

- Available tools: `get_order_status`, `get_shipping_fee` (from `Step5ToolCalling`) and a new tool `search_policy(query)` that calls `DocStore.search` and returns the chunks that pass `RagService.MIN_SIMILARITY`, numbered `[n]`.
- The model decides when to search the policy and when to call the order API.
- Reuse the tool calling loop with the `MAX_STEPS` limit; print every tool call to the console.
- Test questions: one that needs only the policy, one that needs only order data, one that needs both (for example "Order A-1001 has shipped. If the item is defective, can I return it and who pays the shipping?"), and one out of scope.
- Add agent cases to the evaluation: was the right tool chosen for each question.

## Verification

- `mvn -q compile` must pass with no new warnings.
- Run the changed Steps against local Ollama and Postgres. If neither is available, test the logic with a fake OpenAI-compatible server on port 11434, and state clearly which parts were not tested with a real model.
