package com.contentpulse.post.repository;

import com.contentpulse.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<Post> findBySentimentOrderByCreatedAtDesc(Post.Sentiment sentiment, Pageable pageable);

    @Query("""
        SELECT t, COUNT(t) as cnt
        FROM Post p JOIN p.tags t
        WHERE p.createdAt >= :since
        GROUP BY t ORDER BY cnt DESC
        """)
    List<Object[]> findTopTagsSince(LocalDateTime since, Pageable pageable);

    List<Post> findByAiProcessedFalseOrderByCreatedAtAsc();

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLike(Long id);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.likeCount > 0")
    void decrementLike(Long id);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt >= :since")
    long countSince(@Param("since") LocalDateTime since);

    @Query("SELECT p.sentiment, COUNT(p) FROM Post p WHERE p.createdAt >= :since GROUP BY p.sentiment")
    List<Object[]> sentimentCountSince(@Param("since") LocalDateTime since);

    // ── Real daily analytics queries ─────────────────────────

    // Posts count per day for last N days
    @Query("""
        SELECT CAST(p.createdAt AS date), COUNT(p)
        FROM Post p
        WHERE p.createdAt >= :since
        GROUP BY CAST(p.createdAt AS date)
        ORDER BY CAST(p.createdAt AS date) ASC
        """)
    List<Object[]> countByDay(@Param("since") LocalDateTime since);

    // Sentiment count per day
    @Query("""
        SELECT CAST(p.createdAt AS date), p.sentiment, COUNT(p)
        FROM Post p
        WHERE p.createdAt >= :since AND p.sentiment IS NOT NULL
        GROUP BY CAST(p.createdAt AS date), p.sentiment
        ORDER BY CAST(p.createdAt AS date) ASC
        """)
    List<Object[]> sentimentByDay(@Param("since") LocalDateTime since);

    // Likes sum per day
    @Query("""
        SELECT CAST(p.createdAt AS date), SUM(p.likeCount), SUM(p.commentCount), SUM(p.shareCount)
        FROM Post p
        WHERE p.createdAt >= :since
        GROUP BY CAST(p.createdAt AS date)
        ORDER BY CAST(p.createdAt AS date) ASC
        """)
    List<Object[]> engagementByDay(@Param("since") LocalDateTime since);
}
