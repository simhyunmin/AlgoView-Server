package AlgoView_Server.domain.news.repository;

import AlgoView_Server.domain.news.News;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {
}
