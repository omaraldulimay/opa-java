package io.github.open_policy_agent.opa;

import com.sun.net.httpserver.HttpServer;
import io.github.open_policy_agent.opa.openapi.models.shared.Explain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the explain query parameter without needing a real OPA server.
 */
public class OPAClientExplainTest {

    private HttpServer server;
    private final AtomicReference<String> seenQuery = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String q = exchange.getRequestURI().getRawQuery();
            seenQuery.set(q == null ? "" : q);
            byte[] body = "{\"result\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private OPAClient client() {
        return new OPAClient("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Test
    void explainIsNotSentByDefault() throws Exception {
        client().evaluate("example/allow", Map.of("a", 1), Boolean.class);
        assertFalse(seenQuery.get().contains("explain"), seenQuery.get());
    }

    @Test
    void explainIsSentWhenConfigured() throws Exception {
        OPAClient c = client();
        c.setExplain(Explain.NOTES);
        c.evaluate("example/allow", Map.of("a", 1), Boolean.class);
        assertTrue(seenQuery.get().contains("explain=notes"), seenQuery.get());
    }
}
