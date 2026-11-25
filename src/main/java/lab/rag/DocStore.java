package lab.rag;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Stores chunks in the `chunks` table (pgvector). Cosine distance uses the <=> operator. */
public class DocStore implements AutoCloseable {

    public static final String URL =
            System.getenv().getOrDefault("PG_URL", "jdbc:postgresql://localhost:5432/postgres");
    public static final String USER = System.getenv().getOrDefault("PG_USER", "postgres");
    public static final String PASSWORD = System.getenv().getOrDefault("PG_PASSWORD", "postgres");

    public record Hit(String source, String content, double similarity) {
    }

    private final Connection connection;

    public DocStore() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to connect to Postgres at " + URL, e);
        }
    }

    /** Drop and recreate the chunks table. */
    public void reset(int dimension) {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE EXTENSION IF NOT EXISTS vector");
            st.execute("DROP TABLE IF EXISTS chunks");
            st.execute("CREATE TABLE chunks (id SERIAL PRIMARY KEY, source TEXT NOT NULL, "
                    + "content TEXT NOT NULL, embedding vector(" + dimension + ") NOT NULL)");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset chunks table", e);
        }
    }

    public void add(String source, String content, float[] embedding) {
        String sql = "INSERT INTO chunks (source, content, embedding) VALUES (?, ?, ?::vector)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, source);
            ps.setString(2, content);
            ps.setString(3, toVectorLiteral(embedding));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to store chunk", e);
        }
    }

    /** Top-k nearest chunks. similarity = 1 - cosine distance. */
    public List<Hit> search(float[] query, int k) {
        String sql = "SELECT source, content, 1 - (embedding <=> ?::vector) AS similarity "
                + "FROM chunks ORDER BY embedding <=> ?::vector LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String vector = toVectorLiteral(query);
            ps.setString(1, vector);
            ps.setString(2, vector);
            ps.setInt(3, k);
            List<Hit> hits = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    hits.add(new Hit(rs.getString("source"), rs.getString("content"), rs.getDouble("similarity")));
                }
            }
            return hits;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to search chunks", e);
        }
    }

    private static String toVectorLiteral(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            // ignored on close
        }
    }
}
