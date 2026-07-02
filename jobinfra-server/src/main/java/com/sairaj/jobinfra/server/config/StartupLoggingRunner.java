package com.sairaj.jobinfra.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
public class StartupLoggingRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StartupLoggingRunner.class);

    private final Environment env;

    @Value("${jobinfra.app.version:v1.0.0}")
    private String appVersion;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${spring.datasource.url:jdbc:unknown}")
    private String dbUrl;

    @Value("${jobinfra.worker.count:4}")
    private int workerCount;

    public StartupLoggingRunner(Environment env) {
        this.env = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        String javaVersion = System.getProperty("java.version");
        String springVersion = org.springframework.core.SpringVersion.getVersion();
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
        
        String hostname = "unknown";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // ignore
        }

        String[] activeProfiles = env.getActiveProfiles();
        String profile = activeProfiles.length == 0 ? "default" : String.join(", ", activeProfiles);

        long maxMemoryMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
        int cpuCount = Runtime.getRuntime().availableProcessors();

        String maskedDbUrl = maskDbUrl(dbUrl);

        log.info("----------------------------------------------------------");
        log.info("JobInfra Cloud Startup Diagnostics:");
        log.info("----------------------------------------------------------");
        log.info("Version       : {}", appVersion);
        log.info("Java Version  : {}", javaVersion);
        log.info("Spring Version: {}", springVersion);
        log.info("OS            : {}", os);
        log.info("Hostname      : {}", hostname);
        log.info("Server Port   : {}", serverPort);
        log.info("Active Profile: {}", profile);
        log.info("Worker Count  : {}", workerCount);
        log.info("Available RAM : {} MB", maxMemoryMb);
        log.info("CPU Cores     : {}", cpuCount);
        log.info("Database URL  : {}", maskedDbUrl);
        log.info("----------------------------------------------------------");
        log.info("Endpoints:");
        log.info("Swagger UI    : http://localhost:{}/swagger-ui/index.html", serverPort);
        log.info("Health Check  : http://localhost:{}/health", serverPort);
        log.info("Metrics       : http://localhost:{}/api/v1/system/metrics", serverPort);
        log.info("----------------------------------------------------------");
    }

    private String maskDbUrl(String url) {
        if (url == null) return "null";
        if (url.contains("?")) {
            return url.substring(0, url.indexOf("?")) + "?***";
        }
        return url;
    }
}
