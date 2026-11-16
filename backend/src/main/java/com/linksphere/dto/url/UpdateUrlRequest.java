package com.linksphere.dto.url;

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
public class UpdateUrlRequest {

    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private Boolean isActive;

    private Instant expiresAt;
}
