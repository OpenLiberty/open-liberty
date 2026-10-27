/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Parser and matcher for the {@code allowedServerNames} {@code httpDispatcher}
 * attribute (PH71846.5).
 *
 * <p>The attribute is a comma-separated list of host-name patterns. Each entry
 * is either an exact well-formed host name or a leading {@code *.} followed by
 * an anchor of at least two well-formed literal labels. Any other use of
 * {@code *} is rejected.
 *
 * <p>Guards applied at parse time:
 * <ul>
 *   <li><b>Guard 1:</b> bare {@code *} and any entry made only of {@code *}
 *       and {@code .} characters.</li>
 *   <li><b>Guard 2:</b> any entry containing {@code :} (no ports, no IPv6).</li>
 *   <li><b>Guard 3 (anchor too short):</b> a leading {@code *.} entry whose
 *       anchor has fewer than two labels (e.g. {@code *.com}).</li>
 *   <li><b>Wildcard not leading:</b> any {@code *} that is not the whole first
 *       label followed by a dot (e.g. {@code secure-*-helix.example.com},
 *       {@code a*.example.com}, {@code *example.com}, {@code *.*.com}).</li>
 *   <li><b>Too long:</b> a label exceeding 63 characters or a name exceeding
 *       253 characters (RFC 1035).</li>
 *   <li><b>Not well-formed:</b> anything else that fails the well-formedness
 *       rules.</li>
 * </ul>
 *
 * <p>Matching: an exact entry uses {@code equals}; a wildcard entry uses
 * {@code value.endsWith("." + anchor)}. No arrays, no recursion.
 *
 * <p>This class is immutable after construction. All methods are thread-safe.
 */
public final class AllowedServerNamesMatcher {

    /** Maximum characters of a $WSSN value rendered into trace output. */
    private static final int TRACE_MAX_LEN = 256;

    /** RFC 1035: maximum octets in a single DNS label (not counting length byte). */
    private static final int MAX_LABEL_LEN = 63;

    /** RFC 1035: maximum octets in a full domain name in text form (no trailing dot). */
    private static final int MAX_NAME_LEN = 253;

    // -----------------------------------------------------------------------
    // Rejected-entry record
    // -----------------------------------------------------------------------

    /** Reason why a configured entry was rejected at parse time. */
    public enum RejectReason {
        /** Entry is only '*' and '.' characters (guard 1). */
        GUARD_1_WILDCARD_ONLY,
        /** Entry contains ':' -- no ports or IPv6 allowed (guard 2). */
        GUARD_2_CONTAINS_COLON,
        /** Leading '*.' entry whose anchor has fewer than two labels (guard 3). */
        ANCHOR_TOO_SHORT,
        /** Entry contains '*' that is not the whole first label followed by '.' */
        WILDCARD_NOT_LEADING,
        /** A label exceeds 63 characters or the name exceeds 253 characters. */
        TOO_LONG,
        /** Entry is otherwise not a well-formed host name. */
        NOT_WELL_FORMED
    }

    /** An entry that was rejected at parse time, together with the reason. */
    public static final class RejectedEntry {
        private final String entry;
        private final RejectReason reason;

        RejectedEntry(String entry, RejectReason reason) {
            this.entry  = entry;
            this.reason = reason;
        }

        /** The lower-cased entry string as it appeared in the attribute. */
        public String getEntry()        { return entry;  }
        /** Why it was rejected. */
        public RejectReason getReason() { return reason; }

        @Override
        public String toString() {
            return "\"" + entry + "\" rejected (" + reason + ")";
        }
    }

    // -----------------------------------------------------------------------
    // Compiled entry (internal)
    // -----------------------------------------------------------------------

    /** One compiled pattern: either an exact lower-cased name or a wildcard suffix. */
    private static final class CompiledEntry {
        /**
         * For an exact entry: the lower-cased name.
         * For a wildcard entry: "." + lower-cased anchor (used with endsWith).
         */
        final String matchKey;
        /** True for a wildcard entry (leading "*."). */
        final boolean wildcard;

        CompiledEntry(String matchKey, boolean wildcard) {
            this.matchKey = matchKey;
            this.wildcard = wildcard;
        }

        /** The original lower-cased string as it appeared (without the leading "*."). */
        String display() {
            return wildcard ? "*" + matchKey : matchKey;
        }
    }

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private final List<CompiledEntry> entries;
    private final List<RejectedEntry> rejected;

    private static final AllowedServerNamesMatcher EMPTY =
        new AllowedServerNamesMatcher(
            Collections.<CompiledEntry>emptyList(),
            Collections.<RejectedEntry>emptyList());

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    private AllowedServerNamesMatcher(List<CompiledEntry> entries,
                                      List<RejectedEntry> rejected) {
        this.entries  = entries;
        this.rejected = rejected;
    }

    /**
     * Parse a metatype value (one element per configured value; an element may itself
     * be a comma-separated list) and return a matcher.
     * Null or empty returns the empty matcher.
     *
     * @param propertyValues the values from MetatypeUtils.parseStringArray, may be null
     * @return a new matcher (never null)
     */
    public static AllowedServerNamesMatcher parse(String[] propertyValues) {
        if (propertyValues == null || propertyValues.length == 0) {
            return EMPTY;
        }
        StringBuilder sb = new StringBuilder();
        for (String v : propertyValues) {
            if (v != null) {
                if (sb.length() > 0) {
                    sb.append(',');
                }
                sb.append(v);
            }
        }
        return parse(sb.toString());
    }

    /**
     * Parse a comma-separated attribute value and return a matcher.
     * A null or blank value returns the empty matcher (never matches).
     *
     * @param propertyValue raw value of the {@code allowedServerNames} attribute
     * @return a new matcher (never null)
     */
    public static AllowedServerNamesMatcher parse(String propertyValue) {
        if (propertyValue == null || propertyValue.trim().isEmpty()) {
            return EMPTY;
        }

        String[] tokens = propertyValue.split(",", -1);
        List<CompiledEntry> compiled = new ArrayList<CompiledEntry>();
        List<RejectedEntry> rejects  = new ArrayList<RejectedEntry>();

        for (String token : tokens) {
            String raw = token.trim();
            if (raw.isEmpty()) {
                continue;
            }
            // Use Locale.ENGLISH so characters like 'I' lower-case consistently
            // regardless of the JVM's default locale (e.g. Turkish 'i' vs 'I').
            String lower = raw.toLowerCase(Locale.ENGLISH);

            // Guard 2: no colon -- rejects host:port and IPv6 literals.
            if (lower.indexOf(':') >= 0) {
                rejects.add(new RejectedEntry(lower, RejectReason.GUARD_2_CONTAINS_COLON));
                continue;
            }

            // Guard 1: entry consists entirely of '*' and '.' characters.
            if (isOnlyWildAndDot(lower)) {
                rejects.add(new RejectedEntry(lower, RejectReason.GUARD_1_WILDCARD_ONLY));
                continue;
            }

            // The only accepted '*' position is as the whole first label
            // followed by '.', i.e. the entry starts with "*.".
            // Any other '*' placement is WILDCARD_NOT_LEADING.
            boolean leadingWild = lower.startsWith("*.");
            if (!leadingWild && lower.indexOf('*') >= 0) {
                rejects.add(new RejectedEntry(lower, RejectReason.WILDCARD_NOT_LEADING));
                continue;
            }
            if (leadingWild) {
                // The part after "*." must itself contain no '*'.
                String anchor = lower.substring(2);
                if (anchor.indexOf('*') >= 0) {
                    rejects.add(new RejectedEntry(lower, RejectReason.WILDCARD_NOT_LEADING));
                    continue;
                }
                // Guard 3: anchor must have at least two labels.
                // Count dots in anchor: two labels means one dot.
                int dots = 0;
                for (int i = 0; i < anchor.length(); i++) {
                    if (anchor.charAt(i) == '.') dots++;
                }
                if (dots < 1) {
                    // e.g. "*.com" -> anchor = "com", 0 dots, only 1 label.
                    rejects.add(new RejectedEntry(lower, RejectReason.ANCHOR_TOO_SHORT));
                    continue;
                }
                // RFC 1035 length caps on the anchor.
                RejectReason lenReject = checkLengthCaps(anchor);
                if (lenReject != null) {
                    rejects.add(new RejectedEntry(lower, lenReject));
                    continue;
                }
                // Well-formedness of the anchor (no wildcards, so isWellFormed applies directly).
                if (!isWellFormed(anchor)) {
                    rejects.add(new RejectedEntry(lower, RejectReason.NOT_WELL_FORMED));
                    continue;
                }
                // Compile: suffix = "." + anchor  (value must end with this AND be longer).
                compiled.add(new CompiledEntry("." + anchor, true));
            } else {
                // Exact entry: no '*' at all.
                // RFC 1035 length caps.
                RejectReason lenReject = checkLengthCaps(lower);
                if (lenReject != null) {
                    rejects.add(new RejectedEntry(lower, lenReject));
                    continue;
                }
                if (!isWellFormed(lower)) {
                    rejects.add(new RejectedEntry(lower, RejectReason.NOT_WELL_FORMED));
                    continue;
                }
                compiled.add(new CompiledEntry(lower, false));
            }
        }

        return new AllowedServerNamesMatcher(
            Collections.unmodifiableList(compiled),
            Collections.unmodifiableList(rejects));
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /** True when no valid entries were parsed; {@link #matches} will always return false. */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * The entries that were accepted at parse time, in parse order, as lower-cased
     * strings (exact names as-is; wildcard entries prefixed with "*").
     * Never null.
     */
    public List<String> getAcceptedEntries() {
        List<String> result = new ArrayList<String>(entries.size());
        for (CompiledEntry e : entries) {
            result.add(e.display());
        }
        return result;
    }

    /**
     * The entries that were rejected at parse time, in parse order.
     * Never null.
     */
    public List<RejectedEntry> getRejectedEntries() {
        return rejected;
    }

    /**
     * Returns true when {@code value} passes the well-formedness check
     * (including RFC 1035 length caps) and matches at least one configured
     * entry (whole-name, case-insensitive).
     * Returns false for null or empty values.
     *
     * @param value the candidate host-name value (e.g. from $WSSN)
     */
    public boolean matches(String value) {
        if (!isWellFormed(value)) {
            return false;
        }
        if (entries.isEmpty()) {
            return false;
        }
        // Locale.ENGLISH for consistent lower-casing.
        String lower = value.toLowerCase(Locale.ENGLISH);

        for (CompiledEntry e : entries) {
            if (e.wildcard) {
                // "." + anchor: value must be strictly longer and end with the suffix.
                if (lower.length() > e.matchKey.length()
                        && lower.endsWith(e.matchKey)) {
                    return true;
                }
            } else {
                if (lower.equals(e.matchKey)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true when {@code value} is a well-formed host name:
     * <ul>
     *   <li>One or more labels separated by single dots.</li>
     *   <li>Each label: 1 to 63 characters; only ASCII letters, digits, hyphen.
     *       Underscore is not allowed.</li>
     *   <li>Total name length: at most 253 characters (no trailing dot).</li>
     *   <li>No leading or trailing dot; no empty label (..).</li>
     *   <li>No other characters: no colon, slash, at-sign, percent, brackets,
     *       whitespace, control characters or non-ASCII.</li>
     * </ul>
     *
     * @param value the string to check (may be null)
     */
    public static boolean isWellFormed(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        // Total name length cap.
        if (value.length() > MAX_NAME_LEN) {
            return false;
        }
        if (value.charAt(0) == '.' || value.charAt(value.length() - 1) == '.') {
            return false;
        }
        int labelLen = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '.') {
                if (labelLen == 0) {
                    return false; // empty label (..)
                }
                labelLen = 0;
            } else {
                if (!isLabelChar(c)) {
                    return false;
                }
                labelLen++;
                // Per-label length cap.
                if (labelLen > MAX_LABEL_LEN) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a trace-safe rendering of a wire value: control characters are
     * replaced by four-digit hex escapes (backslash-u followed by four hex digits),
     * and the result is capped at {@value #TRACE_MAX_LEN} characters (followed by
     * {@code ...} when truncated).
     * Safe to call with null (returns {@code "(null)"}).
     *
     * @param value the raw wire value
     * @return a printable, log-safe string
     */
    public static String traceSafe(String value) {
        if (value == null) {
            return "(null)";
        }
        StringBuilder sb = new StringBuilder(Math.min(value.length(), TRACE_MAX_LEN + 16));
        int limit = Math.min(value.length(), TRACE_MAX_LEN);
        for (int i = 0; i < limit; i++) {
            char c = value.charAt(i);
            if (c < 0x20 || c > 0x7e) {
                sb.append("\\u");
                String hex = Integer.toHexString(c);
                for (int pad = hex.length(); pad < 4; pad++) {
                    sb.append('0');
                }
                sb.append(hex);
            } else {
                sb.append(c);
            }
        }
        if (value.length() > TRACE_MAX_LEN) {
            sb.append("...(").append(value.length()).append(" chars)");
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /** True if every character in s is '*' or '.'. */
    private static boolean isOnlyWildAndDot(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '*' && c != '.') {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks RFC 1035 length caps without checking character validity.
     * Returns {@link RejectReason#TOO_LONG} if a label exceeds 63 characters
     * or the name exceeds 253 characters; returns null if within caps.
     */
    private static RejectReason checkLengthCaps(String name) {
        if (name.length() > MAX_NAME_LEN) {
            return RejectReason.TOO_LONG;
        }
        int labelLen = 0;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.') {
                labelLen = 0;
            } else {
                labelLen++;
                if (labelLen > MAX_LABEL_LEN) {
                    return RejectReason.TOO_LONG;
                }
            }
        }
        return null;
    }

    /**
     * Valid label characters: ASCII letters, digits, hyphen.
     * Underscore is explicitly excluded.
     */
    private static boolean isLabelChar(char c) {
        return (c >= 'a' && c <= 'z')
            || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9')
            || c == '-';
    }
}
