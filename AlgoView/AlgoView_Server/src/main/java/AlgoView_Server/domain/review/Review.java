package AlgoView_Server.domain.review;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

//사용자 후기 저장 테이블
@Entity
public class Review {
    @Id @GeneratedValue
    private Long id;

    private String contents;
}
