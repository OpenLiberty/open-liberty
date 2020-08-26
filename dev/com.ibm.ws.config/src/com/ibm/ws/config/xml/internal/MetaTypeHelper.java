/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;

public class MetaTypeHelper {

    @Trivial
    public static final List<String> parseValue(String value) {
        List<String> values = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ',') {
                values.add(builder.toString());
                builder.delete(0, builder.length());
            } else if (ch == '\\') {
                if (i + 1 < value.length()) {
                    // add next
                    builder.append(value.charAt(++i));
                } else {
                    // last character - ignore
                }
            } else {
                if (Character.isWhitespace(ch)) {
                    // ignore leading white space
                    if (builder.length() == 0) {
                        continue;
                    }
                    // consume trailing white space
                    i += consumeWhitespace(value, i, builder) - 1;
                } else {
                    builder.append(ch);
                }
            }
        }
        values.add(builder.toString());
        return values;
    }

    @Trivial
    private static int consumeWhitespace(String value, int start, StringBuilder builder) {
        int whitespace = 0;
        for (int i = start; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isWhitespace(ch)) {
                whitespace++;
            } else if (ch == ',') {
                // trailing white space - skip it
                break;
            } else {
                // white space between text - copy it
                for (int j = 0; j < whitespace; j++) {
                    builder.append(value.charAt(start + j));
                }
                break;
            }
        }
        return whitespace;
    }

    /**
     * Escape a metatype value so that it can be processed literally. The
     * expression {@code parseValue(escapeValue(s))} should always contain a
     * single String equal to the input.
     */
    @Trivial
    public static String escapeValue(String s) {
        if (s.isEmpty()) {
            return s;
        }
        if (!s.contains(",") &&
            !s.contains("\\") &&
            !Character.isWhitespace(s.charAt(0)) &&
            !Character.isWhitespace(s.charAt(s.length() - 1))) {
            return s;
        }

        StringBuilder b = new StringBuilder(s.length() * 2);
        int begin = 0;
        int end = s.length();

        // Escape all whitespace at the front of the string.
        for (; begin < end && Character.isWhitespace(s.charAt(begin)); begin++) {
            b.append('\\').append(s.charAt(begin));
        }

        // Skip all whitespace at the end of the string.
        while (end > begin && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }

        for (int i = begin; i < end; i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == ',') {
                b.append(s, begin, i).append('\\');
                begin = i;
            }
        }

        b.append(s, begin, end);

        // Escape all whitespace at the end of the string.
        for (int i = end; i < s.length(); i++) {
            b.append('\\').append(s.charAt(i));
        }

        return b.toString();
    }
}
