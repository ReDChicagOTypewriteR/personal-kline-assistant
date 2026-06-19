package com.alexjoker.quant.marketdata.engine;

import com.alexjoker.quant.marketdata.config.MarketDataEngineProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketDataEngineManager {
    private final MarketDataEngineProperties properties;
    private Process process;
    private boolean externalEngine;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!properties.isEnabled()) {
            log.info("Market data engine is disabled");
            return;
        }
        if (isHealthy()) {
            externalEngine = true;
            log.info("Market data engine already running at {}", baseUrl());
            return;
        }
        try {
            File script = new ClassPathResource("scripts/adata_engine.py").getFile();
            ProcessBuilder builder = new ProcessBuilder(
                    properties.getPythonCommand(),
                    script.getAbsolutePath(),
                    "--host", properties.getHost(),
                    "--port", String.valueOf(properties.getPort()),
                    "--adata-home", resolveAdataHome()
            );
            builder.directory(Path.of(System.getProperty("user.dir")).toFile());
            builder.environment().put("NO_PROXY", "*");
            builder.environment().put("no_proxy", "*");
            builder.redirectErrorStream(true);
            builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            process = builder.start();
            waitUntilHealthy();
            log.info("Market data engine started at {}", baseUrl());
        } catch (Exception ex) {
            log.warn("Failed to start market data engine: {}", ex.getMessage());
            stopProcess();
        }
    }

    @PreDestroy
    public void stop() {
        if (externalEngine) {
            return;
        }
        stopProcess();
    }

    public boolean isAvailable() {
        return properties.isEnabled() && isHealthy();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isExternalEngine() {
        return externalEngine;
    }

    public String baseUrl() {
        return "http://" + properties.getHost() + ":" + properties.getPort();
    }

    private void waitUntilHealthy() {
        Instant deadline = Instant.now().plusSeconds(properties.getStartupTimeoutSeconds());
        while (Instant.now().isBefore(deadline)) {
            if (isHealthy()) {
                return;
            }
            if (process != null && !process.isAlive()) {
                throw new IllegalStateException("market data engine process exited with code " + process.exitValue());
            }
            sleep(300);
        }
        throw new IllegalStateException("market data engine startup timeout");
    }

    private boolean isHealthy() {
        try {
            String status = restClient()
                    .get()
                    .uri("/health")
                    .retrieve()
                    .body(String.class);
            return status != null && status.contains("\"ok\"");
        } catch (Exception ignored) {
            return false;
        }
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(2));
        return RestClient.builder()
                .baseUrl(baseUrl())
                .requestFactory(factory)
                .build();
    }

    private String resolveAdataHome() {
        Path path = Path.of(properties.getAdataHome());
        if (path.isAbsolute() && isAdataHome(path)) {
            return path.normalize().toAbsolutePath().toString();
        }
        for (Path base : candidateBaseDirs()) {
            Path candidate = base.resolve(path).normalize();
            if (isAdataHome(candidate)) {
                return candidate.toAbsolutePath().toString();
            }
        }
        Path fallback = Path.of(System.getProperty("user.dir")).resolve(path).normalize();
        return fallback.toAbsolutePath().toString();
    }

    private List<Path> candidateBaseDirs() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path parent = userDir.getParent() == null ? userDir : userDir.getParent();
        return List.of(
                userDir,
                parent,
                userDir.resolve("..").normalize(),
                userDir.resolve("../..").normalize()
        );
    }

    private boolean isAdataHome(Path path) {
        return path != null && path.resolve("adata").toFile().isDirectory();
    }

    private void stopProcess() {
        if (process == null) {
            return;
        }
        process.destroy();
        sleep(500);
        if (process.isAlive()) {
            process.destroyForcibly();
        }
        process = null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
