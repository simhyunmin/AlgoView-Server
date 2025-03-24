package AlgoView_Server.domain.news.service;

import AlgoView_Server.domain.news.News;
import AlgoView_Server.domain.news.dto.NewsDto;
import AlgoView_Server.domain.news.repository.NewsRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NaverNewsService {

    @Autowired
    private NewsRepository newsRepository;

    public List<NewsDto> parseNaverNewsJson(String jsonResponse, String keyword) {
        List<NewsDto> newsList = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.get("items");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    String title = item.get("title").asText().replaceAll("<[^>]*>", ""); // HTML 태그 제거
                    String description = item.get("description").asText().replaceAll("<[^>]*>", ""); // HTML 태그 제거
                    String link = item.get("link").asText();
                    // keyword는 파라미터로 전달받음
                    NewsDto newsDto = new NewsDto(title, description, link, keyword);
                    newsList.add(newsDto);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패", e);
        }
        return newsList;
    }

    public void saveNewsToDatabase(List<NewsDto> newsList) {
        for (NewsDto newsDto : newsList) {
            News news = News.builder()
                    .title(newsDto.getTitle())
                    .description(newsDto.getDescription())
                    .link(newsDto.getLink())
                    .keyword(newsDto.getKeyword())
                    .build();

            newsRepository.save(news);
        }
    }

    public List<NewsDto> getAllNews() {
        List<NewsDto> newsDtos = newsRepository.findAll()
                .stream()
                .map(n -> new NewsDto(n.getTitle(), n.getDescription(), n.getLink(), n.getKeyword()))
                .collect(Collectors.toList());
        return newsDtos;
    }
}
