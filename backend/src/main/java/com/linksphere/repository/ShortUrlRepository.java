package com.linksphere.repository;

import com.linksphere.entity.ShortUrl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {

    Optional<ShortUrl> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    Page<ShortUrl> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("""
        SELECT s FROM ShortUrl s
        WHERE s.user.id = :userId
          AND (
            LOWER(s.originalUrl) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.shortCode) LIKE LOWER(CONCAT('%', :query, '%'))
          )
        ORDER BY s.createdAt DESC
    """)
    Page<ShortUrl> searchUserUrls(@Param("userId") Long userId, @Param("query") String query, Pageable pageable);

    @Modifying
    @Query("UPDATE ShortUrl s SET s.clickCount = s.clickCount + 1, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    void incrementClickCount(@Param("id") Long id);
}
