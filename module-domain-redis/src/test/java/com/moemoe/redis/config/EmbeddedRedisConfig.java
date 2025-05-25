package com.moemoe.redis.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.util.StringUtils;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@TestConfiguration
@EnableRedisRepositories(basePackages = "com.moemoe.redis", enableKeyspaceEvents = RedisKeyValueAdapter.EnableKeyspaceEvents.ON_STARTUP)
@ComponentScan(basePackages = "com.moemoe.redis.repository")
public class EmbeddedRedisConfig {
    private static final int REDIS_PORT = 6379;
    private static final String REDIS_BINARY_PATH = "binary/redis-server";
    private RedisServer redisServer;

    @PostConstruct
    public void configRedisServer() throws IOException {
        int port = REDIS_PORT;
        if (isProcessRunning(getProcess(port))) {
            port = getAvailablePort();
        }

        redisServer = getRedisServer(port);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedisServer() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    private RedisServer getRedisServer(int port) throws IOException {
        if (isMac()) {
            return new RedisServer(
                    RedisExecProvider.defaultProvider()
                            .override(OS.MAC_OS_X, Architecture.x86_64, REDIS_BINARY_PATH),
                    port);
        } else {
            return new RedisServer(port);
        }
    }

    private boolean isMac() {
        if (!System.getProperty("os.name").equals("Mac OS X")) {
            return false;
        }

        return System.getProperty("os.arch").equals("aarch64") || System.getProperty("os.arch").equals("x86_64");
    }


    /**
     * 사용 가능한 port getter
     *
     * @return 사용 가능한 port
     * @throws IOException 미 사용 port 확인 불가
     */
    private int getAvailablePort() throws IOException {
        for (int port = 10000; port <= 65535; port++) {
            Process process = getProcess(port);
            if (!isProcessRunning(process)) {
                return port;
            }
        }

        throw new RuntimeException("available port is not exist between 10000 and 65535.");
    }

    /**
     * port로 동작되는 process getter
     *
     * @param port port 정보
     * @return port 동작 process
     * @throws IOException shell runtime 실행 exception
     */
    private Process getProcess(int port) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();

        // window인 경우
        if (os.contains("win")) {
            String command = String.format("netstat -ano | find \"LISTEN\" | find \"%d\"", port);
            String[] shell = {"cmd.exe", "/y", "/c", command};
            return Runtime.getRuntime().exec(shell);
        }

        // window가 아닌 경우
        String command = String.format("netstat -nat | grep LISTEN | grep %d", port);
        String[] shell = {"/bin/sh", "-c", command};
        return Runtime.getRuntime().exec(shell);
    }

    /**
     * process 동작 여부 검증
     *
     * @param process 동작 검증될 process
     * @return process 동작 여부
     */
    private boolean isProcessRunning(Process process) {
        String line;
        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (Exception e) {
            throw new RuntimeException("process running read fail.");
        }

        return StringUtils.hasText(stringBuilder.toString());
    }
}