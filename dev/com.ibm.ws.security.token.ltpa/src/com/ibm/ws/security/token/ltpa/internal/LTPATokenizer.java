/*******************************************************************************
 * Copyright (c) 1997, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.token.ltpa.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.auth.InvalidTokenException;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public class LTPATokenizer {

    private static final TraceComponent tc = Tr.register(LTPATokenizer.class);

    private static final char TOKEN_DELIM_PERCENT = '%';
    private static final char USER_DATA_DELIM_DOLLAR = '$';
    private static final char USER_ATTRIB_DELIM_COLON = ':';
    private static final char STRING_ATTRIB_DELIM_CHAR_PIPE = '|';
    private static final String STRING_ATTRIB_DELIM_PIPE = "|";
    private static final char BACKSLASH = '\\';

    /**
     * @param attributes
     * @return
     */
    public static String createUserData(Map<String, ArrayList<String>> attributes) {
        String userData = "";
        StringBuilder sb = new StringBuilder();

        // need to sort the data since this is used for the signature.
        if (attributes.size() > 1) {
            // Use a TreeSet, as it is sorted
            TreeSet<String> elementTreeSet = new TreeSet<String>(attributes.keySet());
            for (String key : elementTreeSet) {
                ArrayList<String> list = attributes.get(key);
                String value = convertArrayListToString(list);
                if (value != null) {
                    sb.append(key).append(USER_ATTRIB_DELIM_COLON).append(value).append(USER_DATA_DELIM_DOLLAR);
                }
            }
        } else {
            for (Entry<String, ArrayList<String>> entry : attributes.entrySet()) {
                String value = convertArrayListToString(entry.getValue());
                sb.append(entry.getKey()).append(USER_ATTRIB_DELIM_COLON).append(value).append(USER_DATA_DELIM_DOLLAR);
            }
        }

        userData = sb.toString();
        // remove the trailing USER_DATA_DELIM and return the rest
        String ret = userData.substring(0, userData.length() - 1);

        return ret;
    }

    /**
     * Parse the String form of a LTPA token and extract the UserData,
     * expiration limit, and the signature.
     *
     * @param tokenStr The String form of a LTPA token
     * @return A list of the strings. 0: The String form of the UserData,
     *         1: Expiration limit of the token, 2: The signature of the token
     */
    @FFDCIgnore(Exception.class)
    protected static final String[] parseToken(String tokenStr) throws InvalidTokenException {
        String[] fields = null;
        int tokenLen = tokenStr.length();
        char c;

        int signBegin = -1, expireBegin = -1;
        // LTPA Token has 2 or 3 fields:
        //    1) Support LTPAToken2 only have userdata, sign
        //    2) Support LTPAToken and LTPAToken2 have userdata, expiration and sign
        //
        // Example1: Only LTPAtoken2
        // expire:1651552864844$u:user\:BasicRealm/steven\\% <sign data>
        // SSO Token has only two : userdata, and expiration

        // Example2: LTPAToken2 and LTPAToken expire
        // expire:1651552864844$u:user\:BasicRealm/steven\\%1651552864844% <sign data>

        // Note: SSO Token has only two : userdata, and expiration

        try {
            for (int i = tokenLen - 1; i > -1; i--) {
                c = tokenStr.charAt(i);
                if (c == TOKEN_DELIM_PERCENT) { // we will encounter one or two of these
                    if (signBegin == -1) {
                        signBegin = i + 1;
                    } else {
                        expireBegin = i + 1;
                        break;
                    }
                }
            }

            // Notice: Liberty only supports LTPAToken2
            if (expireBegin == -1) {
                // Support only LTPAToken2 expire format
                // Token format: userData % sign
                expireBegin = signBegin;
                fields = new String[2];
                fields[0] = tokenStr.substring(0, expireBegin - 1);
                fields[1] = tokenStr.substring(expireBegin, tokenLen);
            } else {
                // two DELIM encountered for LTPAToken and LTPAToken2
                // Token format: userData % expiration % sign
                fields = new String[3];
                fields[0] = tokenStr.substring(0, expireBegin - 1);
                fields[1] = tokenStr.substring(expireBegin, signBegin - 1);
                fields[2] = tokenStr.substring(signBegin, tokenLen);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Error parsing token; " + e);
            }
            throw new InvalidTokenException(e.getMessage(), e);
        }

        return fields;
    }

    /**
     * Given a specified String, parse to find attributes and add to specified Map.
     *
     * @param data    String to parse attribtues from
     * @param attribs Target map to add attributes
     */
    private static void addAttributes(String data, Map<String, ArrayList<String>> attribs) {
        String key;
        String value;
        int keyIndex = 0;
        int dataLen = data.length();
        for (keyIndex = 0; keyIndex < dataLen; keyIndex++) {
            if ((data.charAt(keyIndex) == USER_ATTRIB_DELIM_COLON) && (data.charAt(keyIndex - 1) != BACKSLASH)) {
                key = data.substring(0, keyIndex);
                value = data.substring(keyIndex + 1, dataLen);
                ArrayList<String> list = convertStringToArrayList(key, value);
                if (list != null) {
                    attribs.put(key, list);
                }
            }
        }
    }

    /**
     * Parse the String form of a UserData and get a Map of the UserData.
     *
     * @param userData The String form of a UserData
     * @return A Map of the UserData
     */
    protected static final Map<String, ArrayList<String>> parseUserData(String userData) {
        int tokenLen = userData.length();
        int numOfAttribs = 1; // default has "user" (u) attribute
        int lastDelim = 0;
        int i = 0;
        Map<String, ArrayList<String>> attribs = new HashMap<String, ArrayList<String>>();

        for (i = 0; i < tokenLen; i++) {
            if ((userData.charAt(i) == USER_DATA_DELIM_DOLLAR) && (userData.charAt(i - 1) != BACKSLASH)) {
                numOfAttribs++;
                String data = userData.substring(lastDelim, i);
                lastDelim = i + 1;
                addAttributes(data, attribs);
            }
        }

        // add the last element
        String data = userData.substring(lastDelim, tokenLen);
        addAttributes(data, attribs);

        return attribs;
    }

    private static final String convertArrayListToString(ArrayList<String> input) {
        if (input != null && input.size() > 0) {
            StringBuilder result = new StringBuilder();
            String[] type = input.toArray(new String[input.size()]);
            for (int i = 0; i < type.length; i++) {
                if (i != 0) {
                    result.append(STRING_ATTRIB_DELIM_PIPE);
                }
                result.append(escape(type[i]));
            }
            return result.toString();
        }
        return null;
    }

    /*
     * Convert a String form of an array to the array list
     *
     * @param value The String form of an array
     *
     * @return The array list
     */
    private static final ArrayList<String> convertStringToArrayList(String key, String value) {
        if (value != null && value.length() > 0) {
            ArrayList<String> result = new ArrayList<String>();
            if (key.equals("u")) {
                String nextString = unescape(value);
                    if (nextString != null) {
                        result.add(nextString);
                    }
            } else {
                StringTokenizer st = new StringTokenizer(value, STRING_ATTRIB_DELIM_PIPE);
                while (st.hasMoreTokens()) {
                    String nextString = unescape(st.nextToken());
                    if (nextString != null) {
                        result.add(nextString);
                    }
                }
            }
            
            return result;
        }
        return null;
    }

    /*
     * Remove the delimination of the String form.
     *
     * @param str The String form
     */
    private static final String escape(String str) {
        if (str == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(str.length() * 2);
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);

            switch (c) {
                case TOKEN_DELIM_PERCENT:
                case USER_DATA_DELIM_DOLLAR:
                case USER_ATTRIB_DELIM_COLON:
                case STRING_ATTRIB_DELIM_CHAR_PIPE:
                    sb.append(BACKSLASH);
                    break;
                case BACKSLASH:
                    if (i == len - 1) {
                        sb.append(BACKSLASH);
                    }
                    break;
                default:
                    break;
            }

            sb.append(c);
        }
        return sb.toString();
    }

    /*
     * Add the delimination to the String form.
     *
     * @param str The String form
     */
    private static final String unescape(String str) {
        StringBuilder sb = new StringBuilder(str.length() * 2);
        int len = str.length();
        for (int i = 0; i < len; i++) {
            char c = str.charAt(i);
            if ((c == BACKSLASH) && (i < len - 1)) {
                char d = str.charAt(i + 1);
                if (!((d == USER_DATA_DELIM_DOLLAR) || (d == USER_ATTRIB_DELIM_COLON) || (d == TOKEN_DELIM_PERCENT) || (d == STRING_ATTRIB_DELIM_CHAR_PIPE) || (d == BACKSLASH))) {
                    // if next char is delim, skip this escape char
                    sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

}
