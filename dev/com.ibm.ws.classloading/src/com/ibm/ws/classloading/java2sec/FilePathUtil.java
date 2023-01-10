/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package com.ibm.ws.classloading.java2sec;

/**
 *
 */
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * <p>
 * Encoding for file path name. Especially converting to URL (for CodeBase).
 * </p>
 */
class FilePathUtil {
    /**
     * <p>
     * Encoding Unicode Characters in ASCII format.
     * </p>
     * 
     * <p>
     * The normal characters (between \u0000 and \u007F) are not encoded,
     * except the following:<br><br>
     * <UL>
     *   <li>'='</li>
     *   <li>'='</li>
     *   <li>';'</li>
     *   <li>'?'</li>
     *   <li>'/'</li>
     *   <li>'#'</li>
     *   <li>' '</li>
     *   <li>'<'</li>
     *   <li>'>'</li>
     *   <li>'%'</li>
     *   <li>'"'</li>
     *   <li>'{'</li>
     *   <li>'}'</li>
     *   <li>'|'</li>
     *   <li>'\''</li>
     *   <li>'^'</li>
     *   <li>'['</li>
     *   <li>']'</li>
     *   <li>'`'</li>
     *   <li>Characters between 0 and 32 and 127</li>
     * </UL>
     * <br>
     * </br>
     * Characters will be encoded with the following algorithm (encoded
     * for converting URL).
     * </p>
     * 
     * <pre>
     * Start   End     Required data bits   Binary Byte Sequence
     *                                      (x = data bits)
     * -----   ---     ------------------   --------------------------
     * \u0000  \u007F  7                    0xxxxxxx
     * \u0080  \u07FF  11                   110xxxxx 10xxxxxx
     * \u0800  \uFFFF  16                   1110xxxx 10xxxxxx 10xxxxxx
     * </pre>
     * 
     * @param s
     * @return 
     */
    static String encodeFilePath(String s) {
        int strLen = 0;
        if ((s == null) || ((strLen = s.length()) == 0)) {
            return s;
        }

        StringBuffer buf = new StringBuffer(s.length() + 32);
        for (int i = 0; i < strLen; i++) {
            char c = s.charAt(i);
            if (c == File.separatorChar) {
                buf.append('/');
            } else if (c < 0x80) {  // \u0000 to \u007F
                if (encodingRequired.get(c)) {
                    escape(buf, c);
                } else {
                    buf.append(c);
                }
            } else if (c > 0x7FF) {  // \u0800 to \uFFFF
                escape(buf, (char)(0xE0 | ((c >> 12) & 0xF )));
                escape(buf, (char)(0x80 | ((c >> 6)  & 0x3F)));
                escape(buf, (char)(0x80 | ( c        & 0x3F)));
            } else {  // \u0080 to \u07FF
                escape(buf, (char)(0xC0 | ((c >> 6)  & 0x1F)));
                escape(buf, (char)(0x80 | ( c        & 0x3F)));
            }
        }

        return buf.toString();
    }

    static String decodeFilePath(String s) throws DecodeException {
        int strLen = 0;
        if ((s == null) || ((strLen = s.length()) == 0)) {
            return s;
        }

        StringBuffer buf = new StringBuffer(strLen + 32);

        for (int i = 0; i < strLen; ) {
            char c = s.charAt(i);

            if (c != '%') {  // no decoding
                i += 1;
                buf.append(c);
            } else {  // encoded character
                c = unescpae(s.charAt(i + 1), s.charAt(i + 2));
                i += 3;

                if ((c & 0x80) != 0) {
                    switch (c >> 4) {
                    case 0xC:
                    case 0xD:  // \u0080 to \u07FF
                        {
                            char c1 = s.charAt(i);
                            if (c1 != '%') {
                                throw new DecodeException("Expect '%', but found \'" + c1 + "\'");
                            }
                            c1 = unescpae(s.charAt(i + 1), s.charAt(i + 2));
                            i += 3;

                            buf.append((char) (((c & 0x1F) << 6) | (c1 & 0x3F)));
                        }
                        break;

                    case 0xE:  // \u0800 to \uFFFF
                        {
                            char c1 = s.charAt(i);
                            if (c1 != '%') {
                                throw new DecodeException("Expect '%', but found \'" + c1 + "\'");
                            }
                            c1 = unescpae(s.charAt(i + 1), s.charAt(i + 2));
                            i += 3;

                            char c2 = s.charAt(i);
                            if (c2 != '%') {
                                throw new DecodeException("Expect '%', but found \'" + c1 + "\'");
                            }
                            c2 = unescpae(s.charAt(i + 1), s.charAt(i + 2));
                            i += 3;

                            buf.append((char) (((c & 0xF) << 12) | ((c1 & 0x3F) << 6) | (c2 & 0x3F)));
                        }
                        break;

                    default:
                        throw new DecodeException("Unknown encoding sequence \"" + new String(new char[] {'%', s.charAt(i + 1), s.charAt(i + 2)}) + "\"");
                    }
                } else {
                    buf.append(c);  // \u0000 to \u007F
                }
            }
        }

        return buf.toString();
    }

    static URL filePathToURL(File f) throws MalformedURLException {
        String s = 
            encodeFilePath(f.getAbsolutePath());

        if (!s.startsWith("/")) {
            s = "/" + s;
        }
        if (!s.endsWith("/") && f.isDirectory()) {
            s = s + "/";
        }

        return new URL("file", "", s);
    }

    static URL filePathToURL(File f, String dirChar) throws MalformedURLException {
        String s = 
            encodeFilePath(f.getAbsolutePath());
        if (!s.startsWith("/")) {
            s = "/" + s;
        }
        if (!s.endsWith("/") && f.isDirectory()) {
            s = s + "/";
        }
        if (f.isDirectory()) {
          s = s + dirChar;
        }
        return new URL("file", "", s);
    }

    private static void escape(StringBuffer buf, char c) {
        buf.append('%');
        buf.append(Character.forDigit((c >> 4) & 0xF, 16));
        buf.append(Character.forDigit( c       & 0xF, 16));
    }

    private static char unescpae(char c1, char c2) throws DecodeException {
        int i1 = Character.digit(c1, 16);
        int i2 = Character.digit(c2, 16);

        if ((i1 == -1) || (i2 == -1)) {
            throw new DecodeException("Can not unescape \"" + new String(new char[] {'%', c1, c2}) + "\"");
        }

        return (char) ((i1 << 4) | i2);
    }

    static class DecodeException extends Exception {
        private static final long serialVersionUID = 8237803124662929625L; //@vj1: Take versioning into account if incompatible changes are made to this class 
        private DecodeException(String msg) {
            super(msg);
        }
    }

    private static java.util.BitSet encodingRequired = new java.util.BitSet(256);

    static {
        encodingRequired.set('=');
        encodingRequired.set(';');
        encodingRequired.set('?');
        encodingRequired.set('/');
        encodingRequired.set('#');
        encodingRequired.set(' ');
        encodingRequired.set('<');
        encodingRequired.set('>');
        encodingRequired.set('%');
        encodingRequired.set('"');
        encodingRequired.set('{');
        encodingRequired.set('}');
        encodingRequired.set('|');
        encodingRequired.set('\'');
        encodingRequired.set('^');
        encodingRequired.set('[');
        encodingRequired.set(']');
        encodingRequired.set('`');
        encodingRequired.set(127);

        for (int i = 0; i < 32; i++) {  // All the non-printable characters
            encodingRequired.set(i);
        }
    }

    /**
     * <p>
     * Should not instantiate this class.
     * </p>
     */
    private FilePathUtil() {
    }

}