package com.app;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
@ConfigurationPropertiesScan
public class Application {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        String port = env.getProperty("local.server.port", env.getProperty("server.port", "8080"));
        log.info("==========================================================");
        log.info("  🚀 AI COS Backend Server started successfully!");
        log.info("  📡 Server Port   : {}", port);
        log.info("  🏥 Health Check  : http://localhost:{}/v1/health", port);
        log.info("  📚 Swagger UI     : http://localhost:{}/swagger-ui.html", port);
        log.info("==========================================================");
    }

    /**
     * Exposes the values of {@code backend/.env} as system properties so that
     * application.yml placeholders resolve during local development. Real
     * environment variables always win: entries already present in the
     * environment or on the command line are not overwritten.
     */
    private static void loadDotenv() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) == null && System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }
}
