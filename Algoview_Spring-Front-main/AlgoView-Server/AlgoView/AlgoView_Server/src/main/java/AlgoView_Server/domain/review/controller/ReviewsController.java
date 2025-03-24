package AlgoView_Server.domain.review.controller;

import AlgoView_Server.domain.review.Review;
import AlgoView_Server.domain.review.dto.ReviewDto;
import AlgoView_Server.domain.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ReviewsController {
    private final ReviewService reviewService;

    @GetMapping("/reviews")
    public List<ReviewDto> getReviews() {
        List<ReviewDto> reviews = reviewService.getReviews();
        return reviews;
    }

    @PostMapping("/reviews")
    public void setReview(@RequestParam("name")String name,
                          @RequestParam("contents")String contents) {
        Review review = new Review(name, contents);
        reviewService.setReview(review);
    }
}
