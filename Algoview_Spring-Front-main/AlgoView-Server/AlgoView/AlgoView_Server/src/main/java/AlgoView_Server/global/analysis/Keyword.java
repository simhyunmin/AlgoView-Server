package AlgoView_Server.global.analysis;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class Keyword {
    @Id @GeneratedValue
    private Long id;

    private String keyword;

    private Integer frequency;

    public Keyword(String keyword, Integer frequency) {
        this.keyword = keyword;
        this.frequency = frequency;
    }
}
