package com.contentpulse.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {
    @NotBlank
    @Size(min = 1, max = 280, message = "Content must be between 1 and 280 characters")
    private String content;
}
