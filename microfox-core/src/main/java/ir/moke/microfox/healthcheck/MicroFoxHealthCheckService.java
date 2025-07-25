package ir.moke.microfox.healthcheck;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ir.moke.microfox.MicrofoxEnvironment;
import ir.moke.microfox.exception.MicrofoxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;

public class MicroFoxHealthCheckService {
    private static final Logger logger = LoggerFactory.getLogger(MicroFoxHealthCheckService.class);
    private static final String host = Optional.ofNullable(MicrofoxEnvironment.getEnv("MICROFOX_HEALTH_CHECK_HOST")).orElse("0.0.0.0");
    private static final String port = Optional.ofNullable(MicrofoxEnvironment.getEnv("MICROFOX_HEALTH_CHECK_PORT")).orElse("9091");
    private static final HttpServer server;

    static {
        try {
            server = HttpServer.create(new InetSocketAddress(host, Integer.parseInt(port)), 0);
        } catch (IOException e) {
            throw new MicrofoxException(e);
        }
    }

    public static void start() {
        server.createContext("/health", MicroFoxHealthCheckService::healthCheckController);
        server.start();
        logger.info("Health check HTTP server started at : http://{}:{}/health", host, port);
    }

    private static void healthCheckController(HttpExchange exchange) throws IOException {
        Map<String, HealthStatus> results = HealthCheckManager.checkAll();

        boolean allUp = HealthCheckManager.isHealthy();
        StringBuilder sb = new StringBuilder("{\n  \"status\": \"")
                .append(allUp ? "UP" : "DOWN").append("\",\n  \"details\": {\n");

        for (Map.Entry<String, HealthStatus> entry : results.entrySet()) {
            sb.append("    \"").append(entry.getKey()).append("\": \"")
                    .append(entry.getValue().getStatus()).append("\",\n");
        }

        if (!results.isEmpty()) sb.setLength(sb.length() - 2); // remove last comma
        sb.append("\n  }\n}");

        byte[] body = sb.toString().getBytes();
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(allUp ? 200 : 503, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }
}
