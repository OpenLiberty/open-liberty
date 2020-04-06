/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Describes a single type used in charset negotiation
 * between an HTTP client and server, as described in Section 14.2 and 14.17
 * of RFC2616 (the HTTP/1.1 specification).
 *
 * @since 1.0
 */
public class CharsetRange extends ContentRange {

    /**
     * The literal string value for charset ISO-8859-1.
     */
    final public static String DEFAULT_CHARSET = "ISO-8859-1"; //$NON-NLS-1$

    /**
     * Creates a CharsetRange object with the referenced values.
     *
     * @param type The charset type of this range. May be <code>null</code>.
     * Setting to <code>null</code> or the empty string is the equivalent to
     * setting to "*" (all types). The string is normalized to lowercase and all
     * LWS removed.
     *
     * @param value The quality value of this range.  May be <code>null</code>.
     * Note that the permissible range of values are 0 to 1.0. Setting to
     * <code>null</code> is the equivalent to setting a quality value
     * of '1.0'.
     */
    public CharsetRange(String type, Float value) {
        super(type, value);
    }

    /**
     * Returns the type enclosed by this charset range.  Examples of such a type
     * might be "UTF-8", "ISO-8859-1", or "*".
     *
     * @return The type of this charset range. Never <code>null</code>.  The
     * the returned string will be normalized to lowercase from its original input
     * value.
     */
    @Override
    public String getType() {
        return super.getType();
    }

    /**
     * Parses an Accept-Charset header value into an array of charset ranges.  The
     * returned charset ranges are sorted such that the most acceptable charset
     * is available at ordinal position '0', and the least acceptable at
     * position n-1.<p/>
     *
     * The syntax expected to be found in the referenced <code>value</code>
     * complies with the syntax described in RFC2616, Section 14.2, as
     * described below: <p/>
     *
     * <code>
     * Accept-Charset = "Accept-Charset" ":"
     *         1#( ( charset | "*" )[ ";" "q" "=" qvalue ] )
     * </code>
     *
     * @param value The value to parse.  May be <code>null</code> or empty.
     * If <code>null</code> or empty, a single CharsetRange is returned that represents
     * all types.
     *
     * @return The charset ranges described by the string.  The ranges
     * are sorted such that the most acceptable media is available at ordinal
     * position '0', and the least acceptable at position n-1.
     */
    static public CharsetRange[] parse(String value) {

        RangeParseCallback cb = new RangeParseCallback() {

            public ContentRange rangeParsed(
                    String type, HashMap<String, String[]> parameters,
                    Float qValue, HashMap<String, String[]> extensions)
            {
                return new CharsetRange(type, qValue);
            }

            /**
             * Check if '*' or ISO-8859-1 is mentioned.  If neither mentioned,
             * then append ISO-8859-1 with a qValue of 1.0.  This per the spec
             * that ISO-8859-1 is always implicitly available (unless explicitly
             * excluded).
             */
            public void preSort(ArrayList<ContentRange> rangeList) {

                boolean isoFound = false;
                for (ContentRange range : rangeList) {
                    if (range._type.equals("*") || range._type.equalsIgnoreCase(CharsetRange.DEFAULT_CHARSET)) { //$NON-NLS-1$
                        isoFound = true;
                        break;
                    }
                }
                if (!isoFound)
                    rangeList.add(new CharsetRange(CharsetRange.DEFAULT_CHARSET, null));
            }

            public void postSort(ArrayList<ContentRange> range) { /* empty */
            }
        };

        ContentRange[] range = ContentRange.parse(value, cb);
        CharsetRange[] newRange = (CharsetRange[]) Array.newInstance(CharsetRange.class, range.length);
        System.arraycopy(range, 0, newRange, 0, range.length);

        return newRange;
    }

}
