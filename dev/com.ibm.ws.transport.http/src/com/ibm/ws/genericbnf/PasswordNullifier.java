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
package com.ibm.ws.genericbnf;

/**
 * Class to scan the input value for the "password=" and "client_secret=" key markers
 * and convert the password value to a series of *s.
 */
public class PasswordNullifier {

    /** Search patterns to use for passwords */
    private static final String PASSWORD_PATTERN = "password=";
    private static final String CLIENT_SECRET_PATTERN = "client_secret=";

    /**
     * Scan the input value for the "password=" and "client_secret=" key markers
     * and convert the password value to a series of *s. The delimiter value '&'
     * is used for the expected input format of "key=value&key2=value2".
     * 
     * @param value
     * @return String
     */
    public static String nullifyParams(String value) {
        return nullify(value, (byte) '&');
    }

    /**
     * Scan the input value for the "password=" and "client_secret=" key markers
     * and convert the password value to a series of *s. The delimiter value
     * can be used if the search string is a sequence like
     * "key=value<delim>key2=value2".
     * 
     * @param value
     * @param delimiter
     * @return String
     */
    public static String nullify(String value, byte delimiter) {
        // check to see if we need to null out passwords
        if (null == value) {
            return null;
        }

        String source = value.toLowerCase();
        StringBuilder b = new StringBuilder(value);
        boolean modified = optionallyMaskChars(b, source, delimiter, PASSWORD_PATTERN);
        modified = optionallyMaskChars(b, source, delimiter, CLIENT_SECRET_PATTERN) || modified;

        if (modified) {
            return b.toString();
        }
        return value;
    }

    private static boolean optionallyMaskChars(StringBuilder b, String source, byte delimiter, String pattern) {
        boolean modified = false;
        int i = source.indexOf(pattern);
        while (-1 != i) {
            modified = true;
            // skip past search pattern
            i += pattern.length();

            for (; i < source.length() && delimiter != source.charAt(i); i++) {
                // null out another password value character
                b.setCharAt(i, '*');
            }
            if (i == source.length()) {
                // end of string, just quit
                i = -1;
            } else {
                // look for another instance
                i = source.indexOf(pattern, i);
            }
        }
        return modified;
    }

}
