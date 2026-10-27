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

import com.ibm.ws.http.dispatcher.internal.AllowedServerNamesMatcher;
import com.ibm.ws.http.dispatcher.internal.AllowedServerNamesMatcher.RejectedEntry;
import com.ibm.ws.http.dispatcher.internal.AllowedServerNamesMatcher.RejectReason;

import java.util.List;
import java.util.Locale;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests covering the well-formedness rules and matching behaviour of
 * {@link AllowedServerNamesMatcher}: positive matches, negative cases, parse
 * guards, well-formedness of the candidate value, wildcard scope, RFC 1035
 * length caps, Turkish-locale case, and utility methods.
 */
public class AllowedServerNamesMatcherTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static AllowedServerNamesMatcher matcher(String property) {
        return AllowedServerNamesMatcher.parse(property);
    }

    private static void assertMatches(String property, String value) {
        assertTrue("Expected match for value=[" + value + "] property=[" + property + "]",
            matcher(property).matches(value));
    }

    private static void assertNoMatch(String property, String value) {
        assertFalse("Expected NO match for value=[" + value + "] property=[" + property + "]",
            matcher(property).matches(value));
    }

    private static void assertRejected(String property, RejectReason expectedReason) {
        List<RejectedEntry> rejects = matcher(property).getRejectedEntries();
        assertFalse("Expected at least one rejected entry in [" + property + "]",
            rejects.isEmpty());
        assertEquals("Wrong reason for first rejected entry in [" + property + "]",
            expectedReason, rejects.get(0).getReason());
    }

    // -----------------------------------------------------------------------
    // Section 8 item 3: positive matches
    // -----------------------------------------------------------------------

    @Test
    public void exactMatch() {
        assertMatches("www.example.com", "www.example.com");
    }

    @Test
    public void exactMatchCaseInsensitive() {
        assertMatches("www.example.com", "WWW.EXAMPLE.COM");
        assertMatches("WWW.EXAMPLE.COM", "www.example.com");
    }

    @Test
    public void leadingWildcardSingleLabel() {
        // *.example.com matches a.example.com (one label to the left of anchor)
        assertMatches("*.example.com", "a.example.com");
    }

    @Test
    public void leadingWildcardMultipleLabels() {
        // *.example.com matches a.b.example.com (two labels to the left)
        assertMatches("*.example.com", "a.b.example.com");
    }

    // -----------------------------------------------------------------------
    // Section 8 item 4: negative cases
    // -----------------------------------------------------------------------

    @Test
    public void exactDoesNotMatchParent() {
        assertNoMatch("www.example.com", "example.com");
    }

    @Test
    public void exactDoesNotMatchChild() {
        assertNoMatch("www.example.com", "x.www.example.com");
    }

    @Test
    public void wildcardDoesNotMatchApex() {
        // *.example.com must NOT match example.com
        assertNoMatch("*.example.com", "example.com");
    }

    @Test
    public void wildcardDoesNotMatchAdjacentDomain() {
        // *.example.com must NOT match evilexample.com
        assertNoMatch("*.example.com", "evilexample.com");
    }

    @Test
    public void emptyPropertyNeverMatches() {
        assertFalse(matcher(null).matches("www.example.com"));
        assertFalse(matcher("").matches("www.example.com"));
        assertFalse(matcher("   ").matches("www.example.com"));
    }

    // -----------------------------------------------------------------------
    // Section 8 item 4a: parse guards -- rejection with correct reason
    // -----------------------------------------------------------------------

    @Test
    public void guard1BareAsterisk() {
        assertRejected("*", RejectReason.GUARD_1_WILDCARD_ONLY);
        assertFalse(matcher("*").matches("anything.com"));
    }

    @Test
    public void guard1StarDotStar() {
        assertRejected("*.*", RejectReason.GUARD_1_WILDCARD_ONLY);
        assertFalse(matcher("*.*").matches("www.example.com"));
    }

    @Test
    public void guard2Colon() {
        assertRejected("host.example.com:8443", RejectReason.GUARD_2_CONTAINS_COLON);
        assertFalse(matcher("host.example.com:8443").matches("host.example.com"));
    }

    @Test
    public void guard3AnchorTooShortCom() {
        // *.com -- anchor is "com", only one label
        assertRejected("*.com", RejectReason.ANCHOR_TOO_SHORT);
        assertFalse(matcher("*.com").matches("www.example.com"));
    }

    @Test
    public void guard3AnchorTooShortCorp() {
        assertRejected("*.corp", RejectReason.ANCHOR_TOO_SHORT);
    }

    @Test
    public void wildcardNotLeadingInLabel() {
        // secure-*-helix.example.com -- '*' is not the whole first label
        assertRejected("secure-*-helix.example.com", RejectReason.WILDCARD_NOT_LEADING);
        assertFalse(matcher("secure-*-helix.example.com").matches("secure-x-helix.example.com"));
    }

    @Test
    public void wildcardNotLeadingPartialFirstLabel() {
        // a*.example.com -- '*' inside first label but not alone
        assertRejected("a*.example.com", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void wildcardNotLeadingSuffixAsterisk() {
        // www.example.* -- '*' in last label
        assertRejected("www.example.*", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void wildcardNotLeadingBareStar() {
        // secure* -- single label with embedded '*'
        assertRejected("secure*", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void wildcardNotLeadingNoLeadingDot() {
        // *example.com -- starts with '*' but no '.' follows immediately
        assertRejected("*example.com", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void wildcardNotLeadingDoubleStarInAnchor() {
        // *.*.com -- anchor is "*.com" which itself contains '*'
        assertRejected("*.*.com", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void wildcardNotLeadingDoubleStarInAnchorDeep() {
        // *.*.example.com -- anchor contains '*'
        assertRejected("*.*.example.com", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void wildcardNotLeadingDoubleAsterisk() {
        // **.example.com -- starts with "**" not "*."
        assertRejected("**.example.com", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void wildcardNotLeadingStarInAnchorLabel() {
        // *.a*.example.com -- anchor "a*.example.com" contains '*'
        assertRejected("*.a*.example.com", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void wildcardNotLeadingDotStarEntry() {
        // .*.example.com -- does not start with "*." and contains '*', so the
        // wildcard-not-leading check rejects it before the well-formedness
        // check is reached.
        assertRejected(".*.example.com", RejectReason.WILDCARD_NOT_LEADING);
    }

    @Test
    public void rejectedEntryNeverBecomesAllowAll() {
        AllowedServerNamesMatcher m = matcher("*");
        assertFalse(m.matches("any.example.com"));
        assertFalse(m.matches("x"));
    }

    @Test
    public void rejectedEntryMixedWithValid() {
        // One rejected + one valid: rejected never contaminates the valid entry.
        AllowedServerNamesMatcher m = matcher("*, www.example.com");
        assertFalse(m.matches("any.example.com"));
        assertTrue(m.matches("www.example.com"));
        assertEquals(1, m.getRejectedEntries().size());
    }

    // *.co.uk is accepted (documented public-suffix trap)
    @Test
    public void coUkAcceptedDocumentedTrap() {
        assertTrue("*.co.uk should be accepted (documented trap)",
            matcher("*.co.uk").getRejectedEntries().isEmpty());
        assertMatches("*.co.uk", "anything.co.uk");
        assertMatches("*.co.uk", "a.b.co.uk");
    }

    // -----------------------------------------------------------------------
    // Section 8 item 4b: well-formedness of the $WSSN value
    // -----------------------------------------------------------------------

    @Test
    public void wellFormedLeadingDot() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed(".example.com"));
        assertNoMatch("*.example.com", ".example.com");
    }

    @Test
    public void wellFormedTrailingDot() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("example.com."));
    }

    @Test
    public void wellFormedDoubleDot() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a..b.example.com"));
        assertNoMatch("*.example.com", "a..b.example.com");
    }

    @Test
    public void wellFormedWithPort() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a:80"));
    }

    @Test
    public void wellFormedWithSlash() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a/b"));
    }

    @Test
    public void wellFormedWithAt() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("user@example.com"));
    }

    @Test
    public void wellFormedWithPercent() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a%20b.com"));
    }

    @Test
    public void wellFormedWithSpace() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a b.example.com"));
    }

    @Test
    public void wellFormedWithTab() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a\tb.example.com"));
    }

    @Test
    public void wellFormedWithCR() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a\rb.example.com"));
    }

    @Test
    public void wellFormedWithLF() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a\nb.example.com"));
    }

    @Test
    public void wellFormedNonAscii() {
        // e with acute accent (U+00E9) is non-ASCII
        assertFalse(AllowedServerNamesMatcher.isWellFormed("caf\u00e9.example.com"));
    }

    @Test
    public void wellFormedUnderscore() {
        // Underscore is NOT allowed
        assertFalse(AllowedServerNamesMatcher.isWellFormed("a_b.example.com"));
    }

    @Test
    public void wellFormedIPv6Literal() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("[::1]"));
    }

    @Test
    public void wellFormedNull() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed(null));
    }

    @Test
    public void wellFormedEmpty() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed(""));
    }

    @Test
    public void wellFormedSingleLabel() {
        assertTrue(AllowedServerNamesMatcher.isWellFormed("localhost"));
        assertTrue(AllowedServerNamesMatcher.isWellFormed("a"));
    }

    @Test
    public void wellFormedWithDigitsAndHyphen() {
        assertTrue(AllowedServerNamesMatcher.isWellFormed("my-host-1.example.com"));
    }

    // -----------------------------------------------------------------------
    // Section 8 item 4c: wildcard scope and length caps
    // -----------------------------------------------------------------------

    @Test
    public void wildcardDoesNotMatchApexC4c() {
        assertNoMatch("*.example.com", "example.com");
    }

    @Test
    public void wildcardMatchesGrandchild() {
        assertMatches("*.example.com", "a.b.example.com");
    }

    @Test
    public void wildcardDoesNotMatchLeadingDotValue() {
        assertNoMatch("*.example.com", ".example.com");
    }

    @Test
    public void wildcardDoesNotMatchDoubleDotValue() {
        assertNoMatch("*.example.com", "a..example.com");
    }

    // RFC 1035 label length cap
    @Test
    public void label63CharsWellFormed() {
        String label63 = repeat('a', 63);
        assertTrue("63-char label should be well-formed",
            AllowedServerNamesMatcher.isWellFormed(label63 + ".example.com"));
    }

    @Test
    public void label64CharsNotWellFormed() {
        String label64 = repeat('a', 64);
        assertFalse("64-char label should not be well-formed",
            AllowedServerNamesMatcher.isWellFormed(label64 + ".example.com"));
    }

    @Test
    public void label64EntryRejectedTooLong() {
        String label64 = repeat('a', 64);
        assertRejected(label64 + ".example.com", RejectReason.TOO_LONG);
    }

    // RFC 1035 total name length cap
    @Test
    public void name253CharsWellFormed() {
        // Build a name of exactly 253 chars using 63-char labels and dots.
        // 63 + 1 + 63 + 1 + 63 + 1 + 61 = 253
        String name = repeat('a', 63) + "." + repeat('b', 63) + "." + repeat('c', 63)
                    + "." + repeat('d', 61);
        assertEquals(253, name.length());
        assertTrue("253-char name should be well-formed",
            AllowedServerNamesMatcher.isWellFormed(name));
    }

    @Test
    public void name254CharsNotWellFormed() {
        String name = repeat('a', 63) + "." + repeat('b', 63) + "." + repeat('c', 63)
                    + "." + repeat('d', 62);
        assertEquals(254, name.length());
        assertFalse("254-char name should not be well-formed",
            AllowedServerNamesMatcher.isWellFormed(name));
    }

    @Test
    public void name254EntryRejectedTooLong() {
        String name = repeat('a', 63) + "." + repeat('b', 63) + "." + repeat('c', 63)
                    + "." + repeat('d', 62);
        assertRejected(name, RejectReason.TOO_LONG);
    }

    // Very long value passed to matches() must return false without hanging.
    @Test
    public void veryLongValueReturnsFalse() {
        AllowedServerNamesMatcher m = matcher("*.example.com");
        String longVal = repeat('a', 100000);
        // isWellFormed returns false at the length check; no traversal of 100000 chars.
        assertFalse("100000-char value must return false", m.matches(longVal));
    }

    // Turkish locale -- 'I' must lower-case to 'i', not to dotless 'i'.
    @Test
    public void turkishLocaleMatch() {
        Locale saved = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("tr", "TR"));
            // Without Locale.ENGLISH, "INFO".toLowerCase() in Turkish gives
            // a dotless 'i' for 'I', which would not equal 'i'.
            assertMatches("info.example.com", "INFO.EXAMPLE.COM");
        } finally {
            Locale.setDefault(saved);
        }
    }

    // -----------------------------------------------------------------------
    // getAcceptedEntries
    // -----------------------------------------------------------------------

    @Test
    public void acceptedEntriesExact() {
        AllowedServerNamesMatcher m = matcher("www.example.com, Secure.Example.Com");
        List<String> accepted = m.getAcceptedEntries();
        assertEquals(2, accepted.size());
        assertTrue(accepted.contains("www.example.com"));
        assertTrue(accepted.contains("secure.example.com"));
    }

    @Test
    public void acceptedEntriesWildcard() {
        AllowedServerNamesMatcher m = matcher("*.example.com");
        List<String> accepted = m.getAcceptedEntries();
        assertEquals(1, accepted.size());
        assertEquals("*.example.com", accepted.get(0));
    }

    @Test
    public void acceptedEntriesEmpty() {
        assertTrue(matcher(null).getAcceptedEntries().isEmpty());
        assertTrue(matcher("*").getAcceptedEntries().isEmpty());
    }

    // -----------------------------------------------------------------------
    // traceSafe
    // -----------------------------------------------------------------------

    @Test
    public void traceSafeNormal() {
        assertEquals("www.example.com", AllowedServerNamesMatcher.traceSafe("www.example.com"));
    }

    @Test
    public void traceSafeNull() {
        assertEquals("(null)", AllowedServerNamesMatcher.traceSafe(null));
    }

    @Test
    public void traceSafeCR() {
        String escaped = AllowedServerNamesMatcher.traceSafe("a\rb");
        assertTrue("CR should be escaped", escaped.contains("\\u000d"));
    }

    @Test
    public void traceSafeLF() {
        String escaped = AllowedServerNamesMatcher.traceSafe("a\nb");
        assertTrue("LF should be escaped", escaped.contains("\\u000a"));
    }

    @Test
    public void traceSafeTruncation() {
        String long257 = repeat('a', 257);
        String result = AllowedServerNamesMatcher.traceSafe(long257);
        assertTrue("Should contain truncation marker", result.contains("..."));
    }

    // -----------------------------------------------------------------------
    // BOA customer case
    // -----------------------------------------------------------------------

    @Test
    public void boaExactCase() {
        // BOA uses an exact name, not a wildcard.
        assertMatches(
            "secure-sitbc-helix.ecnp.bankofamerica.com",
            "secure-sitbc-helix.ecnp.bankofamerica.com");
        assertNoMatch(
            "secure-sitbc-helix.ecnp.bankofamerica.com",
            "secure-EVIL-helix.ecnp.bankofamerica.com");
    }

    // -----------------------------------------------------------------------
    // Multiple entries
    // -----------------------------------------------------------------------

    @Test
    public void multipleEntriesBothWork() {
        AllowedServerNamesMatcher m = matcher("www.example.com, secure.example.com");
        assertTrue(m.matches("www.example.com"));
        assertTrue(m.matches("secure.example.com"));
        assertFalse(m.matches("internal.example.com"));
    }

    @Test
    public void emptyTokensSkipped() {
        AllowedServerNamesMatcher m = matcher(",, www.example.com ,,");
        assertTrue(m.matches("www.example.com"));
        assertTrue(m.getRejectedEntries().isEmpty());
    }

    // -----------------------------------------------------------------------
    // Spec R12: matches() only sees the one value passed by the caller
    // -----------------------------------------------------------------------

    @Test
    public void valueWithNewlineIsNotWellFormed() {
        assertFalse(AllowedServerNamesMatcher.isWellFormed("www.example.com\nwww.example.com"));
    }

    // -----------------------------------------------------------------------
    // parse(String[]) overload
    // -----------------------------------------------------------------------

    @Test
    public void parseStringArrayProducesSameMatcherAsParseString() {
        AllowedServerNamesMatcher fromArray  = AllowedServerNamesMatcher.parse(new String[] { "www.example.com", "*.ibm.com" });
        AllowedServerNamesMatcher fromString = AllowedServerNamesMatcher.parse("www.example.com,*.ibm.com");
        assertTrue(fromArray.matches("www.example.com"));
        assertTrue(fromString.matches("www.example.com"));
        assertTrue(fromArray.matches("foo.ibm.com"));
        assertTrue(fromString.matches("foo.ibm.com"));
        assertFalse(fromArray.matches("www.other.com"));
        assertFalse(fromString.matches("www.other.com"));
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private static String repeat(char c, int count) {
        StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) sb.append(c);
        return sb.toString();
    }
}
