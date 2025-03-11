package AlgoView_Server.global.analysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;


@RestController
public class AnalysisController {

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping(value = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processAnalysis(
            @RequestParam("historyFile") MultipartFile historyFile,
            @RequestParam("subscriptionsFile") MultipartFile subscriptionsFile) {

        try {
            // 분석 ID 생성
            String analysisId = UUID.randomUUID().toString();

            // FastAPI 서버 URL (엔드포인트에 맞게 조정)
            String analysisUrl = "http://127.0.0.1:8000/api/v1/analysis/" + analysisId;

            // 히스토리 파일은 그대로 전달 (이미 JSON임)
            byte[] historyBytes = historyFile.getBytes();

            // 구독 파일이 CSV인 경우 JSON으로 변환
            byte[] subscriptionsBytes;
            if (subscriptionsFile.getOriginalFilename().toLowerCase().endsWith(".csv")) {
                subscriptionsBytes = convertCsvToJson(subscriptionsFile);
            } else {
                // 이미 JSON 파일이면 그대로 사용
                subscriptionsBytes = subscriptionsFile.getBytes();
            }

            // 파일 데이터를 multipart로 구성
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

            // HTTP 요청 헤더 설정 (multipart/form-data)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(analysisUrl, requestEntity, Map.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * CSV 파일을 JSON 형식으로 변환
     * 예상 CSV 형식: 채널 ID, 채널 URL, 채널 제목 (헤더 포함)
     * 변환될 JSON 형식: [{"채널 ID": "xxx", "채널 URL": "yyy", "채널 제목": "zzz"}, ...]
     */
    private byte[] convertCsvToJson(MultipartFile csvFile) throws IOException {
        List<Map<String, String>> jsonData = new ArrayList<>();

        // CSV 파일 읽기
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvFile.getInputStream()))) {
            String line;
            String[] headers = null;

            // 첫 번째 줄 (헤더) 읽기
            if ((line = reader.readLine()) != null) {
                headers = line.split(",");
                // 따옴표 제거 및 공백 제거
                for (int i = 0; i < headers.length; i++) {
                    headers[i] = headers[i].replace("\"", "").trim();
                }
            }

            // 데이터 행 읽기
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
            throw new RuntimeException(e);
        }

        // JSON으로 변환
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsBytes(jsonData);
    }

    /**
     * CSV 라인을 파싱하는 메소드 (따옴표로 묶인 값과 쉼표를 올바르게 처리)
     */
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

        // 마지막 값 추가
        result.add(currentValue.toString().replace("\"", "").trim());

        return result.toArray(new String[0]);
    }
}