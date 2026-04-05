package com.learning.backendservice.util;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedReorderedGenerator;

import java.util.UUID;

/**
 * Utility for generating UUID v7 (time-sorted, globally unique, un-guessable).
 *
 * <p>UUID v7 format: {@code {48-bit unix_ts_ms}-{4}-{12-bit rand_a}-{2-bit variant}-{62-bit rand_b}}
 *
 * <p>Properties:
 * <ul>
 *   <li><b>Monotonically increasing</b> — B-Tree index performance matches BIGSERIAL
 *   <li><b>Globally unique</b> — No database coordination required
 *   <li><b>Un-guessable</b> — Prevents ID enumeration attacks on SaaS endpoints
 *   <li><b>RFC 9562 compliant</b> — Industry standard; supported by all DBs
 * </ul>
 *
 * <p>Implementation note: {@link TimeBasedReorderedGenerator} from
 * {@code com.fasterxml.uuid:java-uuid-generator} is thread-safe and
 * handles clock sync and sub-millisecond uniqueness internally.
 */
public final class UuidV7 {

    /** Thread-safe singleton generator (JUG library) */
    private static final TimeBasedReorderedGenerator GENERATOR =
            Generators.timeBasedReorderedGenerator();

    private UuidV7() {}

    /**
     * Generate a new UUID v7.
     *
     * @return UUID v7 instance
     */
    public static UUID generate() {
        return GENERATOR.generate();
    }

    /**
     * Generate a UUID v7 and return it as a lowercase hyphenated string.
     * Convenience method for logging and JSON output.
     *
     * @return e.g. "018e6e9e-1234-7abc-9def-001122334455"
     */
    public static String generateString() {
        return generate().toString();
    }
}
