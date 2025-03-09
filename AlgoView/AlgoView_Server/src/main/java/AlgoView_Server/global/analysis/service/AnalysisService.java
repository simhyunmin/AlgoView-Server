package AlgoView_Server.global.analysis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Component
public class AnalysisService {

    @Value("${analysis.api.url}")
    private String analysisApiUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public AnalysisService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> sendDataForAnalysis(
            String analysisId,
            MultipartFile historyJson,
            MultipartFile subscriptionsJson
    ) {
        try {
            // 엔드포인트 URL 생성
            String url = analysisApiUrl + "/analysis/" + analysisId;

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 멀티파트 폼데이터 생성
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // JSON 파일로 변환하여 추가
            ByteArrayResource historyResource = new ByteArrayResource(historyJson.getBytes()) {
                @Override
                public String getFilename() {
                    return "history.json";
                }
            };

            ByteArrayResource subscriptionsResource = new ByteArrayResource(subscriptionsJson.getBytes()) {
                @Override
                public String getFilename() {
                    return "subscriptions.json";
                }
            };

            // 폼데이터에 파일 추가
            body.add("history_file", historyResource);
            body.add("subscriptions_file", subscriptionsResource);

            // HTTP 요청 엔티티 생성
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // API 호출 및 응답 받기
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Analysis API 호출 중 오류 발생: " + e.getMessage(), e);
        }
    }
}
