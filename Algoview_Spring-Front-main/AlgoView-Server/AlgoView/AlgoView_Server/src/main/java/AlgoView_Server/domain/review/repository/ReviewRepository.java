package AlgoView_Server.domain.review.repository;

import AlgoView_Server.domain.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
}
