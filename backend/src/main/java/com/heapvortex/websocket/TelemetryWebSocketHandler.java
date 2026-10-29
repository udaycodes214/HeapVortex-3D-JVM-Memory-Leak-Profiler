package com.heapvortex.websocket;

import tools.jackson.databind.ObjectMapper;
import com.heapvortex.monitor.JvmMemoryMonitor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryWebSocketHandler extends TextWebSocketHandler {

    private final JvmMemoryMonitor memoryMonitor;
    private final ObjectMapper objectMapper;

    private final Set<WebSocketSession> sessions =
            ConcurrentHashMap.newKeySet();

    public TelemetryWebSocketHandler(
            JvmMemoryMonitor memoryMonitor,
            ObjectMapper objectMapper) {

        this.memoryMonitor = memoryMonitor;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(
            WebSocketSession session) {

        sessions.add(session);

        System.out.println(
                "HeapVortex WebSocket connected: "
                        + session.getId()
        );
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessions.remove(session);

        System.out.println(
                "HeapVortex WebSocket disconnected: "
                        + session.getId()
        );
    }

    @Scheduled(fixedRate = 1000)
    public void broadcastMemory() {

        if (sessions.isEmpty()) {
            return;
        }

        try {

            Map<String, Object> memory = new LinkedHashMap<>();

            memory.put(
                    "timestamp",
                    System.currentTimeMillis()
            );

            memory.put(
                    "heapUsed",
                    memoryMonitor.getHeapUsed()
            );

            memory.put(
                    "heapCommitted",
                    memoryMonitor.getHeapCommitted()
            );

            memory.put(
                    "heapMax",
                    memoryMonitor.getHeapMax()
            );

            memory.put(
                    "heapUsagePercentage",
                    memoryMonitor.getHeapUsagePercentage()
            );

            String json = objectMapper.writeValueAsString(memory);

            for (WebSocketSession session : sessions) {

                if (session.isOpen()) {
                    session.sendMessage(
                            new TextMessage(json)
                    );
                }
            }

        } catch (Exception e) {

            System.err.println(
                    "WebSocket broadcast error: "
                            + e.getMessage()
            );
        }
    }
}