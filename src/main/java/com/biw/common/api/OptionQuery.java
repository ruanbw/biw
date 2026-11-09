package com.biw.common.api;

import com.biw.common.exception.BizException;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * The rules shared by every select-option endpoint: how many rows a picker may return, how many
 * ids it may hydrate, and how a keyword becomes a safe LIKE pattern.
 * <p>
 * These are not page bounds. A picker is a shortlist: the caller either types to search or picks
 * from the first screen, so a bigger limit means the list is being used where a search belongs.
 */
public final class OptionQuery {

    /** A dropdown shows a shortlist; a caller that needs more should search. */
    public static final int MAX_LIMIT = 50;

    /** Highest number of ids one hydration request may ask for. */
    public static final int MAX_IDS = 100;

    private OptionQuery() {
    }

    /** Clamps a requested limit into the range a picker can actually render. */
    public static int limit(int requested) {
        return Math.min(Math.max(requested, 1), MAX_LIMIT);
    }

    /** Drops duplicates and nulls, and refuses a hydration request larger than one page of rows. */
    public static List<Long> distinctIds(List<Long> ids) {
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.size() > MAX_IDS) {
            throw new BizException(HttpStatus.BAD_REQUEST,
                    "ids must contain at most " + MAX_IDS + " items");
        }
        return distinct;
    }

    /**
     * Builds the {@code %keyword%} pattern the option queries bind. Without the escaping a
     * keyword containing {@code %} or {@code _} silently widens the search instead of matching
     * those characters; PostgreSQL's default LIKE escape character is the backslash.
     *
     * @return {@code null} when the caller did not search, which lets the SQL omit the predicate
     */
    public static String pattern(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String escaped = keyword.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
