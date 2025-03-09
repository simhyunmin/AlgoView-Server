package AlgoView_Server.global.analysis.controller;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.*;


@RestController
public class AnalysisController {

//    @PostMapping(value = "/api/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<?> processAnalysis(
//            @RequestParam("historyFile") MultipartFile historyFile,
//            @RequestParam("subscriptionsFile") MultipartFile subscriptionsFile) {
//
//        try {
//            // 분석 ID 생성
//            String analysisId = UUID.randomUUID().toString();
//
//            // HTML 파일 파싱
//            String historyContent = new String(historyFile.getBytes(), StandardCharsets.UTF_8);
//            Map<String, Object> historyData = parseHtmlFile(historyContent);
//
//            // CSV 파일 파싱
//            String subscriptionsContent = new String(subscriptionsFile.getBytes(), StandardCharsets.UTF_8);
//            List<Map<String, String>> subscriptionsData = parseCsvFile(subscriptionsContent);
//
//            // 전송할 JSON 데이터 구성
//            Map<String, Object> requestData = new HashMap<>();
//            requestData.put("history_file", historyData);
//            requestData.put("subscriptions_file", subscriptionsData);
//
//            // FastAPI 서버 URL
//            String analysisUrl = "http://127.0.0.1:8000/api/v1/analysis/" + analysisId;
//
//            // JSON 데이터 전송을 위한 RestTemplate 설정
//            RestTemplate restTemplate = new RestTemplate();
//
//            // HTTP 요청 헤더 설정
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            // HTTP 엔티티 생성
//            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestData, headers);
//
//            // FastAPI 서버로 POST 요청 전송
//            ResponseEntity<Map> response = restTemplate.postForEntity(
//                    analysisUrl,
//                    requestEntity,
//                    Map.class
//            );
//
//            // FastAPI 서버의 응답을 클라이언트에게 전달
//            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(Map.of("status", "error", "message", e.getMessage()));
//        }
//    }
//
//    // HTML 파일 파싱 메서드
//    private Map<String, Object> parseHtmlFile(String htmlContent) {
//        Map<String, Object> result = new HashMap<>();
//
//        try {
//            // Jsoup 라이브러리를 사용하여 HTML 파싱
//            Document doc = Jsoup.parse(htmlContent);
//
//            // 예시: 모든 비디오 제목 추출
//            // 실제 구현은 HTML 구조에 따라 달라짐
//            List<String> videoTitles = new ArrayList<>();
//            Elements titleElements = doc.select(".video-title");// 실제 CSS 선택자로 변경 필요
//
//            for (Element element : titleElements) {
//                videoTitles.add(element.text());
//            }
//
//            result.put("video_titles", videoTitles);
//            // 필요한 다른 데이터 추출 및 추가
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            result.put("error", "HTML 파싱 중 오류 발생: " + e.getMessage());
//        }
//
//        return result;
//    }
//
//    // CSV 파일 파싱 메서드
//    private List<Map<String, String>> parseCsvFile(String csvContent) {
//        List<Map<String, String>> result = new ArrayList<>();
//
//        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
//            // Apache Commons CSV 라이브러리 사용
//            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
//
//            for (CSVRecord record : csvParser) {
//                Map<String, String> row = new HashMap<>();
//                for (String header : csvParser.getHeaderMap().keySet()) {
//                    row.put(header, record.get(header));
//                }
//                result.add(row);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        return result;
//    }


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

            // 파일 데이터를 multipart로 구성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("history_file", new ByteArrayResource(historyFile.getBytes()){
                @Override
                public String getFilename() {
                    return historyFile.getOriginalFilename();
                }
            });
            body.add("subscriptions_file", new ByteArrayResource(subscriptionsFile.getBytes()){
                @Override
                public String getFilename() {
                    return subscriptionsFile.getOriginalFilename();
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

}
