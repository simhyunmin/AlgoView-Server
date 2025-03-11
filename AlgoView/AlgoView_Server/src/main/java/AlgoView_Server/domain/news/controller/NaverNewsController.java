package AlgoView_Server.domain.news.controller;

import AlgoView_Server.domain.news.dto.NewsDto;
import AlgoView_Server.domain.news.service.NaverNewsSearchService;
import AlgoView_Server.domain.news.service.NaverNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.*;
import java.util.*;
import java.net.URLEncoder;

@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:5173")
public class NaverNewsController {

    private final NaverNewsSearchService naverNewsSearchService;
    private final NaverNewsService naverNewsService;

    @GetMapping("/news")
    public ResponseEntity<String> setNewsbyKeywords(/*@RequestParam("keywords")List<String> keywords*/) {
        String clientId = "Pa5Sa3HtfQ9xTZuyRQWP"; //애플리케이션 클라이언트 아이디
        String clientSecret = "PRkum77Vx_"; //애플리케이션 클라이언트 시크릿


        String query = null;
        Integer display = 20;
        List<String> keywords = Arrays.asList("AI", "개발자", "야구");
        for (String keyword : keywords) {
            try {
                query = URLEncoder.encode(keyword, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("검색어 인코딩 실패",e);
            }


            String apiURL = "https://openapi.naver.com/v1/search/news?query=" + query + "&display=" + display;    // JSON 결과


            Map<String, String> requestHeaders = new HashMap<>();
            requestHeaders.put("X-Naver-Client-Id", clientId);
            requestHeaders.put("X-Naver-Client-Secret", clientSecret);
            String responseBody = naverNewsSearchService.get(apiURL,requestHeaders);
            List<NewsDto> newsDtoList = naverNewsService.parseNaverNewsJson(responseBody, keyword);

            for (NewsDto newsDto : newsDtoList) {
                newsDto.setKeyword(keyword);
            }
            naverNewsService.saveNewsToDatabase(newsDtoList);
        }
        return ResponseEntity.ok("뉴스 데이터 저장 완료");
    }

    @GetMapping("/news/list")
    public List<NewsDto> getAllNews() {
        List<NewsDto> allNews = naverNewsService.getAllNews();
        return allNews;
    }
}
