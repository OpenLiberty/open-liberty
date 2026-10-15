/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.osgi.fat.wsloghandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Thread-safe in-memory store for messages captured by TestWsLogHandler.
 * Provides query, verification against routerThing patterns, and reset capabilities.
 */
public class CapturedMessageStore {

    public static class CapturedEntry {
        private final String formattedMessage;
        private final String messageId;
        private final boolean messageHidden;
        private final long timestamp;

        public CapturedEntry(String formattedMessage, boolean messageHidden) {
            this.formattedMessage = formattedMessage;
            this.messageHidden = messageHidden;
            this.timestamp = System.currentTimeMillis();
            this.messageId = parseMessageId(formattedMessage);
        }

        public String getFormattedMessage() { return formattedMessage; }
        public String getMessageId()        { return messageId; }
        public boolean isMessageHidden()    { return messageHidden; }
        public long getTimestamp()          { return timestamp; }

        @Override
        public String toString() { return formattedMessage; }
    }

    public static class VerificationResult {
        private final boolean passed;
        private final String routerThing;
        private final int totalCaptured;
        private final List<String> matchedMessages;
        private final List<String> unmatchedMessages;
        private final List<String> missingPatterns;

        public VerificationResult(boolean passed, String routerThing, int totalCaptured,
                                  List<String> matchedMessages, List<String> unmatchedMessages,
                                  List<String> missingPatterns) {
            this.passed = passed;
            this.routerThing = routerThing;
            this.totalCaptured = totalCaptured;
            this.matchedMessages = matchedMessages;
            this.unmatchedMessages = unmatchedMessages;
            this.missingPatterns = missingPatterns;
        }

        public boolean isPassed()                      { return passed; }
        public String getRouterThing()                 { return routerThing; }
        public int getTotalCaptured()                  { return totalCaptured; }
        public List<String> getMatchedMessages()       { return matchedMessages; }
        public List<String> getUnmatchedMessages()     { return unmatchedMessages; }
        public List<String> getMissingPatterns()       { return missingPatterns; }
    }

    /**
     * Result of a {@link #verifyAbsent} check.
     *
     * <p>Passed when no captured message matched any of the supplied patterns.
     * Failed when at least one captured message matched a pattern that was
     * supposed to be absent.
     */
    public static class AbsenceResult {
        private final boolean passed;
        private final String patterns;
        private final int totalCaptured;
        private final List<String> unexpectedlyMatched;

        public AbsenceResult(boolean passed, String patterns, int totalCaptured,
                             List<String> unexpectedlyMatched) {
            this.passed = passed;
            this.patterns = patterns;
            this.totalCaptured = totalCaptured;
            this.unexpectedlyMatched = unexpectedlyMatched;
        }

        public boolean isPassed()                          { return passed; }
        public String getPatterns()                        { return patterns; }
        public int getTotalCaptured()                      { return totalCaptured; }
        public List<String> getUnexpectedlyMatched()       { return unexpectedlyMatched; }
    }

    private static final List<CapturedEntry> entries = Collections.synchronizedList(new ArrayList<CapturedEntry>());

    public static void addMessage(String formattedMessage, boolean messageHidden) {
        if (formattedMessage != null) {
            entries.add(new CapturedEntry(formattedMessage, messageHidden));
        }
    }

    public static List<CapturedEntry> getEntries() {
        synchronized (entries) {
            return new ArrayList<CapturedEntry>(entries);
        }
    }

    public static List<String> getCapturedMessages() {
        synchronized (entries) {
            List<String> list = new ArrayList<String>(entries.size());
            for (CapturedEntry e : entries) {
                list.add(e.getFormattedMessage());
            }
            return list;
        }
    }

    public static int getCount() {
        return entries.size();
    }

    public static void clear() {
        entries.clear();
    }

    /**
     * Verifies all captured messages against a routerThing pattern string
     * (comma-separated exact IDs and/or wildcard patterns).
     *
     * <p>Checks that:
     * <ol>
     *   <li>Every captured message matches at least one expected pattern (no unexpected captures).</li>
     *   <li>Every expected pattern matches at least one captured message (each message ID / pattern was emitted and received).</li>
     * </ol>
     */
    public static VerificationResult verifyAgainst(String routerThing) {
        List<CapturedEntry> snapshot = getEntries();
        List<String> matched   = new ArrayList<String>();
        List<String> unmatched = new ArrayList<String>();
        List<String> missing   = new ArrayList<String>();

        String[] rawPatterns = (routerThing != null) ? routerThing.split(",") : new String[0];
        List<String> patterns = new ArrayList<String>();
        for (String p : rawPatterns) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                patterns.add(trimmed);
            }
        }

        // 1. Check that all captured entries match at least one expected pattern
        for (CapturedEntry entry : snapshot) {
            if (matchesAnyPattern(entry.getMessageId(), patterns)) {
                matched.add(entry.getFormattedMessage());
            } else {
                unmatched.add(entry.getFormattedMessage());
            }
        }

        // 2. Check that each expected pattern was captured at least once
        for (String pattern : patterns) {
            boolean patternFound = false;
            for (CapturedEntry entry : snapshot) {
                if (matchesPattern(entry.getMessageId(), pattern)) {
                    patternFound = true;
                    break;
                }
            }
            if (!patternFound) {
                missing.add(pattern);
            }
        }

        boolean passed = !patterns.isEmpty()
                && !snapshot.isEmpty()
                && unmatched.isEmpty()
                && missing.isEmpty();

        return new VerificationResult(passed, routerThing, snapshot.size(), matched, unmatched, missing);
    }

    /**
     * Verifies that none of the captured messages match any of the supplied
     * absence patterns (comma-separated exact IDs and/or wildcard patterns).
     *
     * <p>HTTP callers use this to assert that messages which should have been
     * filtered out by the router were never delivered to the handler.
     *
     * <p>Returns PASSED (with an empty {@code unexpectedlyMatched} list) when no
     * captured message matches any pattern.  Returns FAILED with the list of
     * offending messages when at least one match is found.
     */
    public static AbsenceResult verifyAbsent(String patternsCsv) {
        List<CapturedEntry> snapshot = getEntries();
        List<String> unexpectedlyMatched = new ArrayList<String>();

        String[] rawPatterns = (patternsCsv != null) ? patternsCsv.split(",") : new String[0];
        List<String> patterns = new ArrayList<String>();
        for (String p : rawPatterns) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                patterns.add(trimmed);
            }
        }

        for (CapturedEntry entry : snapshot) {
            if (matchesAnyPattern(entry.getMessageId(), patterns)) {
                unexpectedlyMatched.add(entry.getFormattedMessage());
            }
        }

        boolean passed = unexpectedlyMatched.isEmpty();
        return new AbsenceResult(passed, patternsCsv, snapshot.size(), unexpectedlyMatched);
    }

    /**
     * Returns true if msgId matches any pattern in the list.
     */
    public static boolean matchesAnyPattern(String msgId, List<String> patterns) {
        if (msgId == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (matchesPattern(msgId, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Matches a message ID against a single pattern (exact, prefix*, or prefix*Level).
     */
    public static boolean matchesPattern(String msgId, String pattern) {
        if (msgId == null || pattern == null) {
            return false;
        }
        pattern = pattern.trim();
        if (pattern.equals("*")) {
            return true;
        }

        int asteriskIdx = pattern.indexOf('*');
        if (asteriskIdx == -1) {
            return msgId.equals(pattern);
        }

        String prefix = pattern.substring(0, asteriskIdx);
        if (!msgId.startsWith(prefix)) {
            return false;
        }

        if (asteriskIdx == pattern.length() - 1) {
            // prefix* — matches any level
            return true;
        } else if (asteriskIdx == pattern.length() - 2) {
            // prefix*L — level-qualified
            char expectedLevel = pattern.charAt(pattern.length() - 1);
            if (!msgId.isEmpty()) {
                char actualLevel = msgId.charAt(msgId.length() - 1);
                return Character.toUpperCase(actualLevel) == Character.toUpperCase(expectedLevel);
            }
        }
        return false;
    }

    /**
     * Extracts the message ID from a formatted message string using the same
     * first-10-chars / colon-trim rule as the router.
     */
    public static String parseMessageId(String formattedMessage) {
        if (formattedMessage == null) {
            return "";
        }
        String trimmed = formattedMessage.trim();
        if (trimmed.length() >= 10) {
            String msgId = trimmed.substring(0, 10);
            if (msgId.endsWith(":")) {
                msgId = msgId.substring(0, msgId.indexOf(":"));
            }
            int spaceIdx = msgId.indexOf(' ');
            if (spaceIdx > 0) {
                msgId = msgId.substring(0, spaceIdx);
            }
            return msgId.trim();
        } else if (trimmed.contains(":")) {
            return trimmed.substring(0, trimmed.indexOf(":")).trim();
        } else if (trimmed.contains(" ")) {
            return trimmed.substring(0, trimmed.indexOf(" ")).trim();
        }
        return trimmed;
    }
}
