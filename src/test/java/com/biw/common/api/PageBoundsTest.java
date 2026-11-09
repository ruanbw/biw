package com.biw.common.api;

import com.biw.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The window a caller may ask for. Clamping the page and the size separately is not enough: the
 * offset is their product, and a deep enough offset makes PostgreSQL walk rows it then discards.
 */
class PageBoundsTest {

    @Test
    void clampsBothValuesIntoTheirOwnRange() {
        assertThat(PageBounds.of(0, 0)).isEqualTo(new PageBounds(1, 1));
        assertThat(PageBounds.of(-5, 10_000)).isEqualTo(new PageBounds(1, PageBounds.MAX_SIZE));
    }

    @Test
    void acceptsTheDeepestWindowThatStaysWithinTheOffsetCap() {
        long page = PageBounds.MAX_OFFSET / 20 + 1;

        assertThat(PageBounds.of(page, 20).page()).isEqualTo(page);
    }

    @Test
    void rejectsAnOverflowingPageInsteadOfQuietlyPinningIt() {
        // Clamping this to MAX_PAGE would answer a caller that asked for a nonsense page with
        // data from a different one. The offset cap refuses it, which is the honest answer.
        assertThatThrownBy(() -> PageBounds.of(Long.MAX_VALUE, 20))
                .isInstanceOf(BizException.class);
    }

    @Test
    void rejectsAPageWhoseOffsetWouldBeTooDeep() {
        assertThatThrownBy(() -> PageBounds.of(100_000, 100))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("too deep");
    }
}
