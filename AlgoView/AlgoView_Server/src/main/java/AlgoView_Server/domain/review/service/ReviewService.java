package AlgoView_Server.domain.review.service;

import AlgoView_Server.domain.review.Review;
import AlgoView_Server.domain.review.dto.ReviewDto;
import AlgoView_Server.domain.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    public List<ReviewDto> getReviews() {
        List<Review> reviews = reviewRepository.findAll();
        List<ReviewDto> collect = reviews.stream()
                .map(ReviewDto::new)
                .collect(Collectors.toList());
        return collect;
    }

    public void setReview(Review review) {
        reviewRepository.save(review);
    }
}
