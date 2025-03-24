package AlgoView_Server.global.analysis.service;

import AlgoView_Server.global.analysis.Keyword;
import AlgoView_Server.global.analysis.dto.KeywordResponseDto;
import AlgoView_Server.global.analysis.repository.KeywordJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeywordService {
    private final KeywordJpaRepository keywordJpaRepository;
    public void saveKeyword(List<KeywordResponseDto> keywords) {
        for (KeywordResponseDto keyword : keywords) {
            Keyword k = new Keyword(keyword.getKeyword(), keyword.getFrequency());
            keywordJpaRepository.save(k);
        }
    }

    public List<String> getKeywords() {
        List<String> keywords = keywordJpaRepository.findAll().stream()
                .map(a -> a.getKeyword())
                .collect(Collectors.toList());
        return keywords;

    }
}