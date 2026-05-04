package com.contentpulse.post.dto;

import lombok.Data;
import java.util.List;

@Data
public class PostResponse {
    private Long         id;
    private String       content;
    private String       authorName;
    private String       authorHandle;
    private String       sentiment;
    private Double       sentimentScore;
    private List<String> tags;
    private int          likeCount;
    private int          commentCount;
    private int          shareCount;
    private boolean      likedByMe;
    private String       createdAt;
}
