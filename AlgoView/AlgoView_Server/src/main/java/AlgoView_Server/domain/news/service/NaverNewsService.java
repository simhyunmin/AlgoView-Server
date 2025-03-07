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

@Service
public class NaverNewsService {

    @Autowired
    private NewsRepository newsRepository;

    public List<NewsDto> parseNaverNewsJson(String jsonResponse) {
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

                    NewsDto newsDto = new NewsDto(title, description, link);
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
                    .build();

            newsRepository.save(news);
        }
    }
}
