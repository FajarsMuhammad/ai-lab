# ai-lab: belajar AI Engineering dengan Java

Proyek belajar AI Engineer, langkah demi langkah. Semua berjalan **lokal di laptop** (tanpa GPU, tanpa API berbayar): model lewat Ollama, vector DB lewat pgvector di Docker.

## Stack

- Java 27+, Maven, tanpa framework (hanya `java.net.http.HttpClient` dan JDBC)
- Jackson (`jackson-databind`) untuk JSON
- PostgreSQL + pgvector (`org.postgresql:postgresql`)
- Ollama, dengan endpoint OpenAI-compatible di `http://localhost:11434/v1`
  - chat: `llama3.2:3b`
  - embedding: `nomic-embed-text` (alternatif multibahasa: `bge-m3`)

## Setup

```bash
ollama pull llama3.2:3b
ollama pull nomic-embed-text
docker run -d --name pgvector -e POSTGRES_PASSWORD=postgres -p 5432:5432 pgvector/pgvector:pg16
mvn -q compile
mvn -q exec:java -Dexec.mainClass=lab.Step1Hello   # butuh exec-maven-plugin, atau jalankan main dari IDE
```

## Struktur

Semua class ada di `src/main/java/lab/`.

| File | Peran |
|---|---|
| `LlmClient` | Client chat: `chat`, `chatJson` (JSON mode), `chatWithTools` (tool calling). Record `Message`, `ToolCall`, `FunctionCall` |
| `EmbeddingClient` | `embed(String)` dan `embed(List<String>)` (batch, urutan dijaga) |
| `ToolLoop` | Loop tool calling bersama (`MAX_STEPS = 5`), cetak tiap pemanggilan tool, kembalikan `Result(answer, toolsCalled, steps)` |
| `Chunker` | Memecah teks per kalimat sampai `maxChars`, overlap 1 kalimat |
| `DocStore` | Tabel `chunks` di pgvector: `reset`, `add`, `search` (operator `<=>`, jarak cosine) |
| `RagService` | Retrieve top-3, filter `MIN_SIMILARITY`, prompt dengan sitasi `[n]`. Tidak memanggil LLM jika tidak ada konteks |
| `EvalSet` | 15 kasus eval; `expected` = potongan teks wajib di chunk benar, `null` = tidak bisa dijawab. `AGENT_CASES`: 8 kasus agent dengan tool yang diharapkan |
| `Step1Hello` ... `Step4Structured` | Minggu 1: chat, riwayat, temperature, structured output + validasi + retry |
| `Step5ToolCalling` | Minggu 2: loop tool calling (`MAX_STEPS = 5`), tool `get_order_status`, `get_shipping_fee` |
| `Step6EmbeddingBasics`, `Step7VectorSearch` | Minggu 3: cosine similarity, pencarian vektor di pgvector |
| `Step8Rag` | Minggu 4: ingest dokumen kebijakan + RAG lengkap. `DOCUMENT` dan `SOURCE` package-private, dipakai eval |
| `Step9RetrievalEval` | Minggu 5: grid chunk size × threshold, metrik hit@1, hit@3, reject, salah-tolak, salah-terima |
| `Step10JudgeEval` | Minggu 5: LLM-as-judge untuk grounded / halusinasi |
| `Step11Agent` | Minggu 6: agent dengan tool pesanan + `search_policy` (chunk lolos `MIN_SIMILARITY`, bernomor `[n]`) |
| `Step11AgentEval` | Minggu 6: evaluasi pemilihan tool per pertanyaan (benar, terlewat, tidak perlu) |

## Konvensi kode

- Sederhana dan mudah dibaca. Setiap `StepN` adalah program mandiri dengan `main`, yang bisa disalin dan dijalankan langsung.
- Jangan tambahkan framework (Spring AI, LangChain4j) kecuali diminta. Tujuannya memahami apa yang diabstraksikan framework.
- Output model selalu diperlakukan sebagai input eksternal: parse, validasi, dan retry dengan pesan error.
- Hitungan dan data (harga, tanggal, status) datang dari kode Java atau tool, bukan dari model.
- Komentar dan teks console dalam bahasa Indonesia, identifier dalam bahasa Inggris.
- Perubahan sekecil mungkin. Jangan refactor file Step lama saat menambah Step baru; class bersama (`LlmClient`, `DocStore`, dan lainnya) boleh diperluas asalkan Step lama tetap jalan.

## Status

- [x] Minggu 1: dasar LLM API
- [x] Minggu 2: tool calling
- [x] Minggu 3: embeddings + pgvector
- [x] Minggu 4: RAG lengkap
- [x] Minggu 5: evaluasi
- [x] Minggu 6: agent
- [ ] Minggu 7: observability, caching, kontrol biaya (berikutnya)
- [ ] Minggu 8: deploy proyek akhir (Docker) + README

## Minggu 6, agent (selesai)

Buat `Step11Agent`: satu asisten yang menggabungkan Minggu 2 dan Minggu 4.

- Tool yang tersedia: `get_order_status`, `get_shipping_fee` (dari `Step5ToolCalling`) dan tool baru `search_policy(query)` yang memanggil `DocStore.search` lalu mengembalikan chunk lolos `RagService.MIN_SIMILARITY`, bernomor `[n]`.
- Model yang memutuskan kapan mencari kebijakan dan kapan memanggil API pesanan.
- Pakai ulang loop tool calling dengan batas `MAX_STEPS`; cetak setiap pemanggilan tool ke console.
- Pertanyaan uji: satu yang hanya butuh kebijakan, satu yang hanya butuh data pesanan, satu yang butuh keduanya (misalnya "Pesanan A-1001 sudah dikirim, kalau barangnya cacat bisa retur dan siapa yang bayar ongkirnya?"), dan satu di luar cakupan.
- Tambahkan kasus agent ke evaluasi: apakah tool yang dipilih benar untuk tiap pertanyaan.

## Verifikasi

- `mvn -q compile` harus lolos tanpa warning baru.
- Jalankan Step yang diubah terhadap Ollama dan Postgres lokal. Kalau keduanya tidak tersedia, uji logika dengan server tiruan OpenAI-compatible di port 11434, dan sebutkan dengan jelas bagian mana yang belum diuji dengan model asli.
