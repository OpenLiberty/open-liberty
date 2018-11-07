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
package com.ibm.ws.kernel.boot.classloader;

public class URLEncodingUtils {

    /**
     * Encodes a URL path string. This method is suitable only for URL path
     * strings and is unsuitable for other URL components.
     *
     * @param s the string to encode
     * @return the encoded string
     */
    public static String encode(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder builder = null;
        int begin = 0;

        for (int i = 0, length = s.length(); i < length; i++) {
            char ch = s.charAt(i);

            // RFC 2396, Section 3.3.  See also:
            // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5063160
            switch (ch) {
                // lowalpha
                case 'a':
                case 'b':
                case 'c':
                case 'd':
                case 'e':
                case 'f':
                case 'g':
                case 'h':
                case 'i':
                case 'j':
                case 'k':
                case 'l':
                case 'm':
                case 'n':
                case 'o':
                case 'p':
                case 'q':
                case 'r':
                case 's':
                case 't':
                case 'u':
                case 'v':
                case 'w':
                case 'x':
                case 'y':
                case 'z':
                    // upalpha
                case 'A':
                case 'B':
                case 'C':
                case 'D':
                case 'E':
                case 'F':
                case 'G':
                case 'H':
                case 'I':
                case 'J':
                case 'K':
                case 'L':
                case 'M':
                case 'N':
                case 'O':
                case 'P':
                case 'Q':
                case 'R':
                case 'S':
                case 'T':
                case 'U':
                case 'V':
                case 'W':
                case 'X':
                case 'Y':
                case 'Z':
                    // digit
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    // mark
                case '-':
                case '_':
                case '.':
                case '!':
                case '~':
                case '*':
                case '\'':
                case '(':
                case ')':
                    // path_segments
                case '/':
                    // segment
                case ';':
                    // pchar
                case ':':
                case '@':
                case '&':
                case '=':
                case '+':
                case '$':
                case ',':
                    break;

                default:
                    if (builder == null) {
                        builder = new StringBuilder();
                    }

                    builder.append(s, begin, i);
                    begin = i + 1;

                    // Reserved characters are encoded in java.io.DataInput's
                    // "modified UTF-8" format.
                    if (ch <= 0x7f) {
                        appendURLEscapeEncoding(builder, ch);
                    } else if (ch <= 0x7ff) {
                        appendURLEscapeEncoding(builder, 0xc0 | (ch >> 6));
                        appendURLEscapeEncoding(builder, 0x80 | (ch & 0x3f));
                    } else {
                        appendURLEscapeEncoding(builder, 0xe0 | (ch >> 12));
                        appendURLEscapeEncoding(builder, 0x80 | ((ch >> 6) & 0x3f));
                        appendURLEscapeEncoding(builder, 0x80 | (ch & 0x3f));
                    }
                    break;
            }
        }

        if (builder != null) {
            return builder.append(s, begin, s.length()).toString();
        }

        return s;
    }

    // Append a URL escape encoding for the specified character byte.
    private static void appendURLEscapeEncoding(StringBuilder builder, int ch) {
        builder.append('%');
        builder.append(Character.forDigit(ch >> 4, 16));
        builder.append(Character.forDigit(ch & 0xf, 16));
    }

    /**
     * Decodes a URL-encoded path string. For example, an encoded
     * space (%20) is decoded into a normal space (' ') character.
     *
     * @param String encoded - the encoded URL string
     * @return String decoded - the decoded string.
     */
    public static String decode(String s) {
        if (s == null) {
            return null;
        }

        int i = s.indexOf('%');
        if (i == -1) {
            return s;
        }

        StringBuilder builder = new StringBuilder();
        int begin = 0;

        do {
            builder.append(s, begin, i);
            begin = i + 3;

            char ch = (char) Integer.parseInt(s.substring(i + 1, begin), 16);

            if ((ch & 0x80) != 0) {
                // Decode "modified UTF-8".

                if (s.charAt(begin++) != '%') {
                    throw new IllegalArgumentException();
                }

                char ch2 = (char) Integer.parseInt(s.substring(begin, begin + 2), 16);
                begin += 2;

                if ((ch & 0xe0) == 0xc0) {
                    ch = (char) (((ch & 0x1f) << 6) | (ch2 & 0x3f));
                } else if ((ch & 0xf0) == 0xe0) {
                    if (s.charAt(begin++) != '%') {
                        throw new IllegalArgumentException();
                    }

                    char ch3 = (char) Integer.parseInt(s.substring(begin, begin + 2), 16);
                    begin += 2;

                    ch = (char) (((ch & 0x0f) << 12) | ((ch2 & 0x3f) << 6) | (ch3 & 0x3f));
                } else {
                    throw new IllegalArgumentException();
                }
            }

            builder.append(ch);
        } while ((i = s.indexOf('%', begin)) != -1);

        builder.append(s, begin, s.length());

        return builder.toString();
    }
}
