package com.linksphere.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Base62Test {

    @Test
    @DisplayName("Should encode zero correctly")
    void shouldEncodeZero() {
        assertThat(Base62.fromBase10(0)).isEqualTo("0");
    }

    @Test
    @DisplayName("Should encode positive integers into Base62")
    void shouldEncodePositiveLongs() {
        assertThat(Base62.fromBase10(1)).isEqualTo("1");
        assertThat(Base62.fromBase10(61)).isEqualTo("Z");
        assertThat(Base62.fromBase10(62)).isEqualTo("10");
        assertThat(Base62.fromBase10(12519320568L)).isNotBlank();
    }

    @Test
    @DisplayName("Should generate random Base62 string of requested length")
    void shouldGenerateRandomString() {
        String random7 = Base62.generateRandom(7);
        assertThat(random7).hasSize(7).matches("^[a-zA-Z0-9]{7}$");

        String random10 = Base62.generateRandom(10);
        assertThat(random10).hasSize(10).matches("^[a-zA-Z0-9]{10}$");
    }
}
