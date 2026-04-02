package com.contentpulse.post.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_author",    columnList = "author_id"),
    @Index(name = "idx_post_sentiment", columnList = "sentiment"),
    @Index(name = "idx_post_created",   columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 280)
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_handle")
    private String authorHandle;

    // ── AI-generated fields ──────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Sentiment sentiment;

    @Column(name = "sentiment_score")
    private Double sentimentScore;           // 0.0 - 1.0

    @ElementCollection
    @CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    private List<String> tags;

    // ── Engagement ───────────────────────────────────────────
    @Column(name = "like_count")
    private int likeCount = 0;

    @Column(name = "comment_count")
    private int commentCount = 0;

    @Column(name = "share_count")
    private int shareCount = 0;

    // ── AI Processing Status ──────────────────────────────────
    @Column(name = "ai_processed")
    private boolean aiProcessed = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public enum Sentiment { POSITIVE, NEGATIVE, NEUTRAL }
}
