package com.linksphere.service;

import com.linksphere.exception.BadRequestException;
import com.linksphere.exception.DuplicateResourceException;
import com.linksphere.repository.ShortUrlRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortCodeGeneratorTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @InjectMocks
    private ShortCodeGenerator shortCodeGenerator;

    @Test
    @DisplayName("Should generate unique code on first attempt when no collision")
    void shouldGenerateUniqueCodeFirstTry() {
        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(false);

        String code = shortCodeGenerator.generateUniqueCode();

        assertThat(code).hasSize(7);
        verify(shortUrlRepository, atLeastOnce()).existsByShortCode(anyString());
    }

    @Test
    @DisplayName("Should retry when collision occurs")
    void shouldRetryOnCollision() {
        when(shortUrlRepository.existsByShortCode(anyString()))
                .thenReturn(true)  // first attempt collides
                .thenReturn(false); // second attempt succeeds

        String code = shortCodeGenerator.generateUniqueCode();

        assertThat(code).hasSize(7);
        verify(shortUrlRepository, times(2)).existsByShortCode(anyString());
    }

    @Test
    @DisplayName("Should validate valid custom alias")
    void shouldValidateValidAlias() {
        when(shortUrlRepository.existsByShortCode("my-spring-conf")).thenReturn(false);

        String alias = shortCodeGenerator.validateAndNormalizeCustomAlias("my-spring-conf");

        assertThat(alias).isEqualTo("my-spring-conf");
    }

    @Test
    @DisplayName("Should reject reserved keyword as custom alias")
    void shouldRejectReservedKeyword() {
        assertThatThrownBy(() -> shortCodeGenerator.validateAndNormalizeCustomAlias("admin"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("reserved keyword");
    }

    @Test
    @DisplayName("Should reject duplicate custom alias")
    void shouldRejectDuplicateAlias() {
        when(shortUrlRepository.existsByShortCode("promo2026")).thenReturn(true);

        assertThatThrownBy(() -> shortCodeGenerator.validateAndNormalizeCustomAlias("promo2026"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    @DisplayName("Should reject alias with invalid characters")
    void shouldRejectInvalidCharacters() {
        assertThatThrownBy(() -> shortCodeGenerator.validateAndNormalizeCustomAlias("bad alias!@#"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("only contain alphanumeric");
    }
}
