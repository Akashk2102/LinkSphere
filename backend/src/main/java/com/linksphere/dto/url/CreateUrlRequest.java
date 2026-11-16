package com.linksphere.dto.url;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUrlRequest {

    @NotBlank(message = "Original URL is required")
    @Pattern(
        regexp = "^(https?://)([-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b)([-a-zA-Z0-9()@:%_+.~#?&/=]*)$",
        message = "Must be a valid HTTP or HTTPS URL"
    )
    private String originalUrl;

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String customAlias;

    private Instant expiresAt;
}
