package AlgoView_Server.global.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/analysis")
public class SseProxyController {

    private final WebClient webClient;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Autowired
    public SseProxyController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:8000").build();
        System.out.println("SseProxyController initialized");  // 초기화 로그 추가
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> uploadFilesForAnalysis(
            @RequestParam("historyFile") MultipartFile historyFile,
            @RequestParam("subscriptionsFile") MultipartFile subscriptionsFile) {
        try {
            String analysisId = "test"; // 임의의 analysisId 설정
            System.out.println("Starting file upload for analysisId: " + analysisId);  // 파일 업로드 시작 로그

            byte[] historyBytes = historyFile.getBytes();
            byte[] subscriptionsBytes = subscriptionsFile.getOriginalFilename().toLowerCase().endsWith(".csv") ?
                    convertCsvToJson(subscriptionsFile) : subscriptionsFile.getBytes();

            MultiValueMap<String, Object> body = createMultipartBody(historyBytes, subscriptionsBytes);
            System.out.println("Sending request to external service for analysis...");  // 외부 서비스 요청 로그

            webClient.post()
                    .uri("/api/v1/analysis/" + analysisId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(body))
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> System.out.println("Analysis started: " + response),
                            error -> System.err.println("Error starting analysis: " + error.getMessage())
                    );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Analysis started");
            response.put("analysisId", analysisId);
            return response;

        } catch (Exception e) {
            System.err.println("Error during file upload: " + e.getMessage());  // 파일 업로드 오류 로그
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return response;
        }
    }

    // 추가된 GET 엔드포인트
    @GetMapping(value = "/events/{analysisId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToAnalysisResults(@PathVariable String analysisId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitter.onCompletion(() -> {
            emitters.remove(analysisId);
            System.out.println("SSE emitter completed for analysisId: " + analysisId);  // 완료 로그
        });
        emitter.onTimeout(() -> {
            emitters.remove(analysisId);
            System.out.println("SSE emitter timed out for analysisId: " + analysisId);  // 타임아웃 로그
        });
        emitter.onError(e -> {
            emitters.remove(analysisId);
            System.err.println("SSE emitter error for analysisId: " + analysisId + " : " + e.getMessage());  // 에러 로그
        });

        emitters.put(analysisId, emitter);

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(Map.of("status", "connected", "analysisId", analysisId)));

            // 분석 서버에서 SSE 스트리밍을 시작하는 메소드 호출
            subscribeToAnalysisStream(analysisId, emitter);
        } catch (IOException e) {
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private void subscribeToAnalysisStream(String analysisId, SseEmitter emitter) {
        System.out.println("Subscribing to analysis stream for analysisId: " + analysisId);  // 분석 스트림 구독 시작 로그
        webClient.get()
                .uri("/api/v1/analysis-stream/" + analysisId)  // 수정된 부분
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(25))
                .subscribe(
                        data -> {
                            try {
                                String jsonData = data.startsWith("data:") ? data.substring(5).trim() : data;
                                ObjectMapper mapper = new ObjectMapper();
                                Map<String, Object> eventData = mapper.readValue(jsonData, Map.class);

                                String eventType = eventData.containsKey("type") ?
                                        eventData.get("type").toString() : "update";

                                emitter.send(SseEmitter.event()
                                        .name(eventType)
                                        .data(eventData));

                                if ("complete".equals(eventType)) {
                                    emitter.complete();
                                }
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(Map.of("error", error.getMessage())));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> emitter.complete()
                );
    }

    private MultiValueMap<String, Object> createMultipartBody(byte[] historyBytes, byte[] subscriptionsBytes) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("history_file", new ByteArrayResource(historyBytes) {
            @Override
            public String getFilename() {
                return "history.json";
            }
        });
        body.add("subscriptions_file", new ByteArrayResource(subscriptionsBytes) {
            @Override
            public String getFilename() {
                return "subscriptions.json";
            }
        });
        return body;
    }

    private byte[] convertCsvToJson(MultipartFile csvFile) throws IOException {
        List<Map<String, String>> jsonData = new ArrayList<>();
        String[] headers = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream()))) {
            String line;
            if ((line = reader.readLine()) != null) {
                headers = line.split(",");
                for (int i = 0; i < headers.length; i++) {
                    headers[i] = headers[i].replace("\"", "").trim();
                }
            }

            while ((line = reader.readLine()) != null) {
                String[] values = parseCSVLine(line);
                if (headers != null && values.length >= headers.length) {
                    Map<String, String> entry = new HashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        entry.put(headers[i], values[i]);
                    }
                    jsonData.add(entry);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("CSV parsing error: " + e.getMessage(), e);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(jsonData);
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if (currentChar == '\"') {
                inQuotes = !inQuotes;
            } else if (currentChar == ',' && !inQuotes) {
                result.add(currentValue.toString().replace("\"", "").trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(currentChar);
            }
        }

        result.add(currentValue.toString().replace("\"", "").trim());
        return result.toArray(new String[0]);
    }
}
