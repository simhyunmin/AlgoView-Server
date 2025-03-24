package AlgoView_Server.global.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
public class KeywordResponseDto {
    private String keyword;

    private Integer frequency;

    public KeywordResponseDto(String keyword, Integer frequency) {
        this.keyword = keyword;
        this.frequency = frequency;
    }
}
