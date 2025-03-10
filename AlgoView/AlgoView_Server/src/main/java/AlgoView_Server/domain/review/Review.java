package AlgoView_Server.domain.review;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

//사용자 후기 저장 테이블
@Entity
@Getter
@NoArgsConstructor
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String contents;

    public Review(String name, String contents) {
        this.name = name;
        this.contents = contents;
    }
}
