package com.biw.common.api;

import com.biw.common.error.BizCode;
import com.biw.common.exception.BizException;

/**
 * Sanitised {@code page} / {@code size} for a list query.
 * <p>
 * Clamping alone is not enough: {@code page * size} is the offset, and a large enough product
 * asks PostgreSQL to walk that many rows before discarding them. {@code user:read} is enough to
 * trigger it, so the product is capped as well — a caller that deep is paging through data it
 * should be filtering instead.
 */
public record PageBounds(long page, long size) {

    /** Highest page accepted, so the offset cannot overflow a {@code long}. */
    public static final long MAX_PAGE = 100_000;

    /** Highest page size accepted. */
    public static final long MAX_SIZE = 100;

    /** Highest offset accepted, i.e. rows the database may be asked to skip. */
    public static final long MAX_OFFSET = 10_000;

    public static PageBounds of(long page, long size) {
        long boundedPage = Math.min(Math.max(page, 1), MAX_PAGE);
        long boundedSize = Math.min(Math.max(size, 1), MAX_SIZE);
        if ((boundedPage - 1) * boundedSize > MAX_OFFSET) {
            throw new BizException(BizCode.PAGE_TOO_DEEP);
        }
        return new PageBounds(boundedPage, boundedSize);
    }
}
