package AlgoView_Server.global.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AnalysisStreamController {

    private final WebClient webClient;

    @Autowired
    public AnalysisStreamController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://127.0.0.1:8000")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    @GetMapping(value = "/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> getAnalysisStream(
            @RequestParam("historyFileName") String historyFileName,
            @RequestParam("subscriptionsFileName") String subscriptionsFileName) {

        // 클라이언트가 SSE 연결을 설정하면, 여기에서는 이전에 업로드된 파일에 대한 분석 결과를 스트리밍합니다.
        String sessionId = UUID.randomUUID().toString();
        System.out.println("SSE 연결 설정: " + sessionId);

        return Flux.create(emitter -> {
            // SSE 연결이 설정됨을 알리는 이벤트
            emitter.next(ServerSentEvent.<String>builder()
                    .id(UUID.randomUUID().toString())
                    .event("connection-established")
                    .data("{\"status\":\"connected\",\"message\":\"분석 준비 완료\"}")
                    .build());
        });
    }

    @PostMapping(value = "/analyze")
    public ResponseEntity<Map<String, String>> uploadAndAnalyze(
            @RequestParam("historyFile") MultipartFile historyFile,
            @RequestParam("subscriptionsFile") MultipartFile subscriptionsFile,
            SseEmitter emitter) {

        String analysisId = UUID.randomUUID().toString();
        System.out.println("분석 시작: " + analysisId);

        try {
            // 파일 바이트 읽기
            byte[] historyBytes = historyFile.getBytes();
            byte[] subscriptionsBytes = getSubscriptionsBytes(subscriptionsFile);

            // MultipartData 생성
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("history_file", new ByteArrayResource(historyBytes) {
                @Override
                public String getFilename() {
                    return "history.json";
                }
            });
            builder.part("subscriptions_file", new ByteArrayResource(subscriptionsBytes) {
                @Override
                public String getFilename() {
                    return "subscriptions.json";
                }
            });

            // 파이썬 서버의 스트리밍 엔드포인트로 요청 (비동기 처리)
            webClient.post()
                    .uri("/api/v1/analysis-stream/{analysisId}", analysisId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(builder.build()))
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(result -> {
                        System.out.println("분석 결과 수신: " + result);

                        // 클라이언트의 연결이 유지되고 있는 경우에만 결과 전송
                        try {
                            SseEmitter.SseEventBuilder event = SseEmitter.event()
                                    .id(UUID.randomUUID().toString())
                                    .name("analysis-result")
                                    .data(result);
                            emitter.send(event);
                        } catch (Exception e) {
                            System.err.println("SSE 전송 중 오류: " + e.getMessage());
                        }
                    })
                    .doOnComplete(() -> {
                        System.out.println("분석 ID " + analysisId + "의 모든 결과 전송 완료");
                        try {
                            emitter.complete();
                        } catch (Exception e) {
                            System.err.println("SSE 완료 처리 중 오류: " + e.getMessage());
                        }
                    })
                    .doOnError(e -> {
                        System.err.println("분석 중 오류 발생: " + e.getMessage());
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            System.err.println("SSE 오류 처리 중 추가 오류: " + ex.getMessage());
                        }
                    })
                    .subscribe();

            Map<String, String> response = new HashMap<>();
            response.put("status", "processing");
            response.put("analysisId", analysisId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // SSE 연결을 위한 SseEmitter 객체 제공
    @GetMapping("/analyze/stream")
    public SseEmitter streamResults() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // 연결 타임아웃 설정
        emitter.onTimeout(() -> {
            System.out.println("SSE 연결 타임아웃");
            emitter.complete();
        });

        // 연결 오류 처리
        emitter.onError((e) -> {
            System.err.println("SSE 연결 오류: " + e.getMessage());
            emitter.complete();
        });

        // 연결 완료 처리
        emitter.onCompletion(() -> {
            System.out.println("SSE 연결 완료");
        });

        return emitter;
    }

    // CSV를 JSON으로 변환하거나 필요한 처리를 수행하는 헬퍼 메서드
    private byte[] getSubscriptionsBytes(MultipartFile file) throws IOException {
        if (file.getOriginalFilename().toLowerCase().endsWith(".csv")) {
            return convertCsvToJson(file);
        } else {
            return file.getBytes();
        }
    }

    // CSV를 JSON으로 변환하는 메서드
    private byte[] convertCsvToJson(MultipartFile file) throws IOException {
        // CSV 파일 내용 읽기
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
        CsvToJsonConverter converter = new CsvToJsonConverter();
        String jsonString = converter.convert(reader);
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }
}

// CSV를 JSON으로 변환하는 유틸리티 클래스
class CsvToJsonConverter {
    public String convert(BufferedReader reader) throws IOException {
        CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
        List<Map<String, String>> list = new ArrayList<>();

        for (CSVRecord record : csvParser) {
            Map<String, String> map = new HashMap<>();
            csvParser.getHeaderMap().forEach((key, value) -> map.put(key, record.get(key)));
            list.add(map);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(list);
    }
}