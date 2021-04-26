/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import java.io.CharConversionException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LDAPUtils {
    /** pattern to match asterisk, left paren, right paren, backslash and any character > 127 */
    private static final Pattern FILTER_CHARS_TO_ESCAPE = Pattern.compile("[*\\(\\)\\\\\0\u0080-\uFFFF]");

    /**
     * Escape a term for use in an LDAP filter.
     *
     * @param term
     * @return
     */
    public static String escapeLDAPFilterTerm(String term) {
        if (term == null)
            return null;
        Matcher m = FILTER_CHARS_TO_ESCAPE.matcher(term);
        StringBuffer sb = new StringBuffer(term.length());
        while (m.find()) {
            final String replacement = escapeFilterChar(m.group().charAt(0));
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Escape the metacharacters and the higher order characters.
     * Since this routine converts individual characters to bytes,
     * it may generate incorrect output when operating on character
     * sequences representing codepoints in any of the following
     * unicode codepoints blocks:
     * <ul><li><code>0xd800..0xdb7f</code> (High Surrogates)
     * </li><li><code>0xdb80..0xdc00</code> (High Private Use Surrogates)
     * </li><li><code>0xdc00..0xdfff</code> (Low Surrogates)
     * </li><li><code>0x10000..0x10ffff</code> (Supplementary Private Use Area-B)
     * </li></ul>
     * This should not matter, since these aren't characters in
     * any character set. If these are required, they will have
     * to be processed as surrogate pairs, and not character by
     * character.
     * <p>
     * Note: the ranges above are unicode codepoints, and not
     * Java char values. This routine <em>will</em> work with any
     * Java chars from any human language string expressible in
     * Java (at least in Java version 7.0).
     *
     * @param ch the character (or high/low surrogate) to be escaped
     * @return the escaped form of the character
     */
    private static String escapeFilterChar(char ch) {
        switch (ch) {
            case '*':
                return "\\2a";
            case '(':
                return "\\28";
            case ')':
                return "\\29";
            case '\\':
                return "\\5c";
            default:
                // must be a character > 128, so process it as UTF-8 bytes
                try {
                    byte[] bytes = Character.toString(ch).getBytes(StandardCharsets.UTF_8);
                    switch (bytes.length) {
                        case 1:
                            return String.format("\\%02x", bytes[0]);
                        case 2:
                            return String.format("\\%02x\\%02x", bytes[0], bytes[1]);
                        case 3:
                            return String.format("\\%02x\\%02x\\%02x", bytes[0], bytes[1], bytes[2]);
                        default:
                            throw new CharConversionException("No character should map to more than three UTF-8 bytes");
                    }
                } catch (CharConversionException e) {
                    // auto-FFDC
                    return "";
                }
        }
    }
}
