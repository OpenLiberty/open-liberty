/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.util;

/**
 * Utilities for parsing strings commonly found in HTTP protocol transactions.
 */
public class StringUtil {

    /**
     * Returns the value described in {@link #splitPair(String, char)}, as if
     * the char argument were set to <code>/</code>.  Additionally, returns
     * <code>{ "*", "*" }</code> if the string argument is set to <code>"*"</code>.
     *
     * @see #splitPair(String, char)
     */
    static public String[] splitAcceptPairAllowingSingleAsterisk(String token) {
        // Java programmatic clients will by default use this single asterisk in
        // their accept header. Allow this (even though it is contrary to HTTP
        // specs).
        if (token.equals("*"))
            return new String[] { "*", "*" };
        else
            return StringUtil.splitPair(token, '/');
    }

    /**
     * Returns the value described in {@link #splitPair(String, char, boolean)},
     * as if the boolean argument were set to <code>true</code>.
     *
     * @see #splitPair(String, char, boolean)
     */
    static public String[] splitPair(String token, char ch) {

        return StringUtil.splitPair(token, ch, true);
    }

    /**
     * Returns a two element array containing the left and right-hand sides
     * of a token as separated by the specified <code>ch</code>.  The token
     * is split on the first occurrence of the specified character.  Any LWS
     * occurring on either end of the split tokens is removed.
     *
     * @param token The parameter pair. Must be non-<code>null</code>. If
     * <code>mustExist</code> is <code>true</code>, then the token must contain
     * an embedded reference to <code>ch</code>.

     * @param ch The character to split on.
     *
     * @param mustExist <code>true</code> if the split character <code>ch</code>
     * must exist in <code>token</code>, <code>false</code> if not.
     *
     * @return The split pair of strings.  If <code>mustExist</code> is <code>true</code>,
     * the LHS of the split is found in element '0', the RHS in element '1'.  If
     * <code>mustExist</code> is <code>false</code> and the split character is not
     * found, the trimmed token is found in element '0', and the empty string in
     * element '1'.
     */
    static public String[] splitPair(String token, char ch, boolean mustExist) {

        int ndx = token.indexOf(ch);
        if (ndx == -1) {
            if (mustExist)
                throw new IllegalArgumentException(String.format("token is expected to be in 'name%svalue' format", new Character(ch).toString())); //$NON-NLS-1$

            return new String[] { token.trim(), "" }; //$NON-NLS-1$
        }

        String[] retVal = new String[2];
        retVal[0] = token.substring(0, ndx).trim();
        retVal[1] = token.substring(ndx + 1).trim();

        return retVal;
    }

    private StringUtil() {
        super();
        throw new UnsupportedOperationException("Instance creation is not supported."); //$NON-NLS-1$
    }
}
