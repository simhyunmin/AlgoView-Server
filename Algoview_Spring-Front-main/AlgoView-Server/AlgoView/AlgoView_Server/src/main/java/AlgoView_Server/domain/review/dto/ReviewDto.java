package AlgoView_Server.domain.review.dto;

import AlgoView_Server.domain.review.Review;
import lombok.Getter;

@Getter
public class ReviewDto {
    private String name;
    private String contents;

    public ReviewDto(Review review) {
        this.name = review.getName();
        this.contents = review.getContents();
    }
}
