package com.contentpulse.post.service;

import com.contentpulse.auth.repository.UserRepository;
import com.contentpulse.kafka.producer.PostEvent;
import com.contentpulse.kafka.producer.PostEventProducer;
import com.contentpulse.post.dto.CreatePostRequest;
import com.contentpulse.post.dto.PostResponse;
import com.contentpulse.post.entity.Post;
import com.contentpulse.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository     postRepo;
    private final UserRepository     userRepo;
    private final PostEventProducer  producer;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("dd MMM, HH:mm");

    // ── Create Post ──────────────────────────────────────────
    @Transactional
    public PostResponse createPost(CreatePostRequest req, String username) {
        var user = userRepo.findByUsername(username).orElseThrow();

        Post post = Post.builder()
            .content(req.getContent())
            .authorId(user.getId())
            .authorName(user.getUsername())
            .authorHandle("@" + user.getUsername())
            .aiProcessed(false)
            .build();

        post = postRepo.save(post);

        // Publish to Kafka → Analytics Consumer will call Groq AI
        PostEvent event = PostEvent.builder()
            .postId(post.getId())
            .content(post.getContent())
            .authorId(post.getAuthorId())
            .authorName(post.getAuthorName())
            .authorHandle(post.getAuthorHandle())
            .timestamp(LocalDateTime.now())
            .build();

        producer.publishPostCreated(event);
        log.info("Post created and published to Kafka: id={}", post.getId());

        return toResponse(post, false);
    }

    // ── Get Feed ─────────────────────────────────────────────
    public Page<PostResponse> getFeed(int page, int size, String sentiment, String username) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Post> posts;
        if (sentiment != null && !sentiment.isBlank()) {
            try {
                Post.Sentiment s = Post.Sentiment.valueOf(sentiment.toUpperCase());
                posts = postRepo.findBySentimentOrderByCreatedAtDesc(s, pageable);
            } catch (IllegalArgumentException e) {
                posts = postRepo.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            posts = postRepo.findAllByOrderByCreatedAtDesc(pageable);
        }

        return posts.map(p -> toResponse(p, false));
    }

    // ── Like / Unlike ────────────────────────────────────────
    @Transactional
    public void toggleLike(Long postId, String username) {
        Post post = postRepo.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        // Simple toggle — in production, track per-user likes in a separate table
        postRepo.incrementLike(postId);

        PostEvent event = PostEvent.builder()
            .postId(post.getId())
            .content(post.getContent())
            .authorId(post.getAuthorId())
            .timestamp(LocalDateTime.now())
            .build();
        producer.publishPostLiked(event);
    }

    // ── Delete Post ──────────────────────────────────────────
    @Transactional
    public void deletePost(Long postId, String username) {
        Post post = postRepo.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post not found"));

        var user = userRepo.findByUsername(username).orElseThrow();
        if (!post.getAuthorId().equals(user.getId()))
            throw new AccessDeniedException("Cannot delete another user's post");

        postRepo.delete(post);
    }

    // ── AI result callback (called by Analytics Consumer) ────
    @Transactional
    public void updateAiAnalysis(Long postId, String sentiment,
                                  Double score, java.util.List<String> tags) {
        postRepo.findById(postId).ifPresent(post -> {
            try {
                post.setSentiment(Post.Sentiment.valueOf(sentiment));
                post.setSentimentScore(score);
                post.setTags(tags);
                post.setAiProcessed(true);
                postRepo.save(post);
                log.info("AI analysis saved for post id={} sentiment={}", postId, sentiment);
            } catch (Exception e) {
                log.error("Failed to update AI analysis for post {}: {}", postId, e.getMessage());
            }
        });
    }

    // ── Mapper ───────────────────────────────────────────────
    private PostResponse toResponse(Post p, boolean likedByMe) {
        PostResponse res = new PostResponse();
        res.setId(p.getId());
        res.setContent(p.getContent());
        res.setAuthorName(p.getAuthorName());
        res.setAuthorHandle(p.getAuthorHandle());
        res.setSentiment(p.getSentiment() != null ? p.getSentiment().name() : "NEUTRAL");
        res.setSentimentScore(p.getSentimentScore());
        res.setTags(p.getTags());
        res.setLikeCount(p.getLikeCount());
        res.setCommentCount(p.getCommentCount());
        res.setShareCount(p.getShareCount());
        res.setLikedByMe(likedByMe);
        res.setCreatedAt(p.getCreatedAt() != null ? p.getCreatedAt().format(FMT) : "");
        return res;
    }
}
