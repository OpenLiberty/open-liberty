/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.config;

import com.ibm.ws.kernel.instrument.serialfilter.digest.Checksums;

import java.util.logging.Logger;

/**
 * A utility enum to determine and convert the format of a specifier.
 * The possible formats are as follows:
 * <dl>
 *     <dt>{@link #PREFIX}</dt><dd>e.g. com.acme.* or com.acme.util.Concurrent*</dd>
 *     <dt>{@link #CLASS}</dt><dd>e.g. com.acme.internal.Widget</dd>
 *     <dt>{@link #DIGEST}</dt><dd>e.g. com.acme.internal.Widget:<digest string></dd>
 *     <dt>{@link #METHOD}</dt><dd>e.g. com.acme.internal.Widget#readExternal</dd>
 *     <dt>{@link #METHOD_PREFIX}</dt><dd>e.g. com.acme.internal.Widget#read*</dd>
 *     <dt>{@link #UNKNOWN}</dt><dd>any other unrecognised format</dd>
 * </dl>
 */
enum SpecifierFormat {
    PREFIX,
    CLASS,
    DIGEST,
    METHOD,
    METHOD_PREFIX,
    UNKNOWN;

    static final char DIGEST_DELIM_CHAR = ':';
    static final char METHOD_DELIM_CHAR = '#';
    static final char INTERNAL_END_CHAR = '!';
    static final char WILDCARD_CHAR = '*';
    static final String DIGEST_DELIM = "" + DIGEST_DELIM_CHAR;
    static final String METHOD_DELIM = "" + METHOD_DELIM_CHAR;
    static final String INTERNAL_DIGEST_DELIM = INTERNAL_END_CHAR + DIGEST_DELIM;
    static final String INTERNAL_METHOD_DELIM = INTERNAL_END_CHAR + METHOD_DELIM;

    /**
     * Provide an internal format suitable for storing in a trie and looking
     * up prefix matches. For non-prefix formats append a unique terminator
     * character so they never show up as a prefix match for longer strings.
     * <br>
     * e.g. A CLASS entry for <code>foo.bar.Baz</code> should not match when
     * searching for <code>foo.bar.Bazzy</code> but a PREFIX entry of
     * <code>foo.bar.Baz*</code> should match a search for either class.
     * <br>
     * Also, an empty METHOD_PREFIX <code>foo.bar.Baz#*</code> should not
     * match a search for the class <code>foo.bar.Baz</code> but should match
     * a search for any of its methods.
     */
    static String internalize(String s) {
        // assume this is a valid external format string
        OUTER_THIS_LOOP:
        for (int i = 0; i < s.length(); i++) {
            switch (s.charAt(i)) {
                case DIGEST_DELIM_CHAR:
                    return s.substring(0, i) + INTERNAL_END_CHAR + s.substring(i);
                case METHOD_DELIM_CHAR:
                    s = s.substring(0, i) + INTERNAL_END_CHAR + s.substring(i);
                    break OUTER_THIS_LOOP;
            }
        }
        // only reaches here if s is not in DIGEST format
        final int endIndex = s.length() - 1;
        return (s.charAt(endIndex) == WILDCARD_CHAR) ? s.substring(0, endIndex) : (s + INTERNAL_END_CHAR);
    }

    static String externalize(String s) {
        // assume this is a valid internal format string
        OUTER_THIS_LOOP:
        for (int i = 0; i < s.length() - 1; i++) {
            switch (s.charAt(i)) {
                case INTERNAL_END_CHAR:
                    s = s.substring(0, i) + s.substring(i + 1);
                    if (s.charAt(i) == DIGEST_DELIM_CHAR)
                        return s;
                    break OUTER_THIS_LOOP;
            }
        }
        final int endIndex = s.length() - 1;
        return (endIndex >= 0 && s.charAt(endIndex) == INTERNAL_END_CHAR) ? s.substring(0, endIndex) : (s + WILDCARD_CHAR);
    }

    static SpecifierFormat fromString(String s) {
        if (s.isEmpty()) return UNKNOWN;

        ParseState state = ParseState.BEFORE_IDENTIFIER;
        try {
            for (int offset = 0; offset < s.length() && !!! state.isTerminal; /* AFTERTHOUGHT at end of loop */) {
                int cp = s.codePointAt(offset);

                state = state.getNextState(cp);

                offset += Character.charCount(cp);
                state.lookAhead(s, offset);
            }
            return state.format;
        } catch (ParseException e) {
            return UNKNOWN;
        }
    }

    private enum ParseState {
        BEFORE_IDENTIFIER(UNKNOWN, false) {
            @Override
            ParseState getNextState(int codepoint) {
                switch (codepoint) {
                    case '*': return AFTER_PREFIX;
                    default:  return Character.isJavaIdentifierStart(codepoint) ? IN_IDENTIFIER : PARSE_ERROR;
                }
            }
        },
        IN_IDENTIFIER(CLASS, false) {
            @Override
            ParseState getNextState(int codepoint) {
                switch (codepoint) {
                    case '.': return BEFORE_IDENTIFIER;
                    case ':': return BEFORE_DIGEST;
                    case '*': return AFTER_PREFIX;
                    case '#': return BEFORE_METHOD;
                    default:  return Character.isJavaIdentifierPart(codepoint) ? this : PARSE_ERROR;
                }
            }
        },
        AFTER_PREFIX(PREFIX, true) {
            @Override
            void lookAhead(String s, int offset) throws ParseException {
                if (offset != s.length()) {
                    Logger.getLogger(SpecifierFormat.class.getName()).severe("String contains unexpected content at offset " + offset + ": " + s);
                    throw new ParseException();
                }
            }
        },
        BEFORE_METHOD(UNKNOWN, false) {
            @Override
            ParseState getNextState(int codepoint) {
                switch (codepoint) {
                    case '*': return AFTER_METHOD_PREFIX;
                    case '<': return SPECIAL_METHOD_CLINIT_OR_INIT;
                    default:  return Character.isJavaIdentifierStart(codepoint) ? IN_METHOD : PARSE_ERROR;
                }
            }
        },
        SPECIAL_METHOD_CLINIT_OR_INIT(UNKNOWN, false) {
            @Override
            ParseState getNextState(int codepoint) {
                switch (codepoint) {
                    case '*': return AFTER_METHOD_PREFIX; // allow wildcard here to match all special methods
                    case 'c':
                    case 'i': return SPECIAL_METHOD;
                    default:  return PARSE_ERROR;
                }
            }
        },
        SPECIAL_METHOD(METHOD, true) {
            @Override
            void lookAhead(String s, int offset) throws ParseException {
                String method = s.substring(offset - 2);
                if (method.equals("<clinit>") || method.equals("<init>")) return;
                throw new ParseException();
            }
        },
        IN_METHOD(METHOD, false) {
            @Override
            ParseState getNextState(int codepoint) {
                switch (codepoint) {
                    case '*': return AFTER_METHOD_PREFIX;
                    default:  return Character.isJavaIdentifierPart(codepoint) ? IN_METHOD : PARSE_ERROR;
                }
            }
        },
        AFTER_METHOD_PREFIX(METHOD_PREFIX, true) {
            @Override
            void lookAhead(String s, int offset) throws ParseException {
                if (offset != s.length()) {
                    Logger.getLogger(SpecifierFormat.class.getName()).severe("String contains unexpected content at offset " + offset + ": " + s);
                    throw new ParseException();
                }
            }
        },
        BEFORE_DIGEST(DIGEST, true) {
            @Override
            void lookAhead(String s, int offset) throws ParseException {
                final String checksum = s.substring(offset);
                if (!!!Checksums.isValidChecksum(checksum)) {
                    Logger.getLogger(SpecifierFormat.class.getName()).severe("String contains invalid checksum: " + s);
                    throw new ParseException();
                }
            }
        },
        PARSE_ERROR(UNKNOWN, true) {
            @Override
            void lookAhead(String s, int offset) throws ParseException {
                throw new ParseException();
            }
        };

        final SpecifierFormat format;
        final boolean isTerminal;

        ParseState(SpecifierFormat format, boolean isTerminal) {
            this.format = format;
            this.isTerminal = isTerminal;
        }

        ParseState getNextState(int codepoint) {
            return PARSE_ERROR;
        }

        void lookAhead(String s, int offset) throws ParseException {}
    }

    private static class ParseException extends Exception{}
}
