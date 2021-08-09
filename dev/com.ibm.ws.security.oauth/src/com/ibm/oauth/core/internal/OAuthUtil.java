/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.oauth.core.internal;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * Utility functions
 * 
 */
public class OAuthUtil {
    private static TraceComponent tc = Tr.register(OAuthUtil.class);

    static final String JCEPROVIDER_IBM = "IBMJCE";

    static final String SECRANDOM_IBM = "IBMSecureRandom";

    static final String SECRANDOM_SHA1PRNG = "SHA1PRNG";

    static final String UTF_ENCODING = "UTF-8";

    /**
     * Gets the first value from the values array of a Map <String, String[]>
     * 
     * @param key
     * @param m
     *            Map <String, String[]>
     * @return
     */

    private static final java.util.List<String> sensitives = java.util.Arrays.asList(new String[] {"client_secret", "sharedKey"});
    
    @Sensitive
    public static String getValueFromMap(String key, Map<String, String[]> m) {
        String result = null;

        String values[] = (String[]) m.get(key);
        if (values != null && values.length > 0) {
            result = values[0];
        }

        if (tc.isDebugEnabled()) {
            if (sensitives.contains(key)) {
              if (result != null) {
                Tr.debug(tc, "getValueFromMap("+key+") returns [*****]");
              } else {
                Tr.debug(tc, "getValueFromMap("+key+") returns [null]");
              }
            } else {
              Tr.debug(tc, "getValueFromMap("+key+") returns ["+result+"]");
            }
        }
        return result;
    }

    /**
     * Converts an array of strings to a space delimited string
     * 
     * @param array
     * @return
     */
    public static String arrayToSpaceString(String[] array) {
        StringBuffer result = new StringBuffer();

        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                result.append(array[i]);

                if (i < array.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString();
    }

    /**
     * Generates a random alphanumeric string of length n to be used for OAuth
     * 2.0 keys, tokens, secrets etc
     * 
     * @param length
     * @return
     */
    public static String getRandom(int length) {
        StringBuffer result = new StringBuffer(length);
        final char[] chars = new char[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
                'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
                'U', 'V', 'W', 'X', 'Y', 'Z',
                'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
                'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
                'u', 'v', 'w', 'x', 'y', 'z'
        };
        Random r = getRandom();

        for (int i = 0; i < length; i++) {
            int n = r.nextInt(62);
            result.append(chars[n]);
        }

        String retVal = result.toString();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getRandom("+length+") returns ["+retVal+"]");
        }
        return retVal;
    }

    static Random getRandom() {
        Random result = null;
        try {
            if (Security.getProvider(JCEPROVIDER_IBM) != null) {
                result = SecureRandom.getInstance(SECRANDOM_IBM);
            } else {
                result = SecureRandom.getInstance(SECRANDOM_SHA1PRNG);
            }
        } catch (Exception e) {
            result = new Random();
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getRandom() returns ["+result.getClass().getName()+"]");
        }
        return result;
    }

    public static String generateUUID() {
        String retVal = UUID.randomUUID().toString();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "generateUUID returns ["+retVal+"]");
        }
        return retVal;
    }

    /**
     * Validate URI by constructing the URI class and checking if it is
     * absolute. We'll know if it's a valid URI if the exception is not thrown.
     * 
     * @param strUri
     * @param requireHttps
     * @return
     */
    public static boolean validateUri(String strUri)
    {
        boolean valid = false;

        try {
            if (strUri != null) {
                URI uri = new URI(strUri);
                if (uri.isAbsolute()) {
                    valid = true;
                }
            }
        } catch (URISyntaxException e) {
            valid = false;
        }

        if (tc.isDebugEnabled()) {
            //inbound calls to this method aren't stripping any possible secrets out of the string when tracing, so I'm not doing it here either
            Tr.debug(tc, "validateUri("+strUri+") returns ["+valid+"]");
        }
        return valid;
    }

    public static String stripQueryAndFragment(String uri)
    {
        String result = uri;

        if (validateUri(uri)) {
            int query = result.indexOf("?");
            if (query != -1) {
                result = result.substring(0, query);
            }

            int hash = result.indexOf("#");
            if (hash != -1) {
                result = result.substring(0, hash);
            }
        }

        if (tc.isDebugEnabled()) {
            //inbound calls to this method aren't stripping any possible secrets out of the string when tracing, so I'm not doing it here either
            Tr.debug(tc, "stripQueryAndFragment("+uri+") returns ["+result+"]");
        }
        return result;
    }

    public static String getQuery(String uri)
    {
        String result = uri;

        try {
            URI u = new URI(uri);
            result = u.getQuery();
        } catch (URISyntaxException e) {
            // invalid uri, return null
        }
        if (tc.isDebugEnabled()) {
            //inbound calls to this method aren't stripping any possible secrets out of the string when tracing, so I'm not doing it here either
            Tr.debug(tc, "getQuery("+uri+") returns ["+result+"]");
        }
        return result;
    }

    /**
     * Encodes each parameter in the provided query. Expects the query argument to be the query string of a URL with parameters
     * in the format: param=value(&param2=value2)*
     * 
     * @param query
     * @return
     */
    public static String encodeQuery(String query) {
        if (query == null) {
            return null;
        }
        StringBuilder rebuiltQuery = new StringBuilder();

        // Encode parameters to mitigate XSS attacks
        String[] queryParams = query.split("&");
        for (String param : queryParams) {
            try {
                String rebuiltParam = URLEncoder.encode(param, UTF_ENCODING);
                int equalIndex = param.indexOf("=");
                if (equalIndex > -1) {
                    String name = param.substring(0, equalIndex);
                    String value = (equalIndex < (param.length() - 1)) ? param.substring(equalIndex + 1) : "";
                    rebuiltParam = URLEncoder.encode(name, UTF_ENCODING) + "=" + URLEncoder.encode(value, UTF_ENCODING);
                }
                if (!rebuiltParam.isEmpty()) {
                    rebuiltQuery.append(rebuiltParam + "&");
                }
            } catch (UnsupportedEncodingException e) {
                // Do nothing - UTF-8 should be supported
            }
        }
        // Remove trailing '&' character
        if (rebuiltQuery.length() > 0 && rebuiltQuery.charAt(rebuiltQuery.length() - 1) == '&') {
            rebuiltQuery.deleteCharAt(rebuiltQuery.length() - 1);
        }
        String retVal = rebuiltQuery.toString();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "encodeQuery("+query+") returns ["+retVal+"]");
        }
        return retVal;
    }

    public static String getCurrentStackTraceString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
