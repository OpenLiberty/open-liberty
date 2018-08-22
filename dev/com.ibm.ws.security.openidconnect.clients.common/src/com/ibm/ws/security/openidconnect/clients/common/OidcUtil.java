/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;

import javax.security.auth.Subject;
import javax.servlet.http.Cookie;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.webcontainer.security.CookieHelper;

/**
 * Collection of utility methods for String to byte[] conversion.
 */
public class OidcUtil {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(OidcUtil.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    static final char[] chars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z',
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J',
            'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T',
            'U', 'V', 'W', 'X', 'Y', 'Z' };
    static final int iCharlength = chars.length;
    static final int iTwoCharsLenth = chars.length * chars.length;

    static final String JCEPROVIDER_IBM = "IBMJCE";

    static final String SECRANDOM_IBM = "IBMSecureRandom";

    static final String SECRANDOM_SHA1PRNG = "SHA1PRNG";

    public static final int TIMESTAMP_LENGTH = 15;
    public static final int RANDOM_LENGTH = 9;
    public static final int STATEVALUE_LENGTH = TIMESTAMP_LENGTH + RANDOM_LENGTH;

    /**
     * This method will return a <code>String</code> that is compliant with xs:id simple
     * type which is used to declare SAML identifiers for assertions, requests, and responses.
     * The String returned will contain 33 chars and will always start with '_', the probability
     * to have a duplicate is 62^32.
     *
     * @return <code>String</code> compliant with xs:id
     */
    @Trivial
    public static String generateRandomID() {
        return "_" + generateRandom(32);
    }

    @Trivial
    public static String generateRandom() {
        return generateRandom(32);
    }

    @Trivial
    // see defect 218708
    public static String generateRandom(int iCharCnt) {
        char[] strChars = new char[iCharCnt];
        Random r = getRandom();
        for (int i = 0; i < iCharCnt;) {
            int rnd = r.nextInt(iTwoCharsLenth);
            int firstChar = rnd / iCharlength;
            strChars[i++] = chars[firstChar];
            if (i < iCharCnt) { // in case, it's an odo number
                int secondChar = rnd % iCharlength;
                strChars[i++] = chars[secondChar];
            }
        }
        return new String(strChars);
    }

    @Trivial
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
        return result;
    }

    @Trivial
    public static StringBuffer dumpStackTrace(Throwable cause, int iLimited) {
        StackTraceElement[] stackTrace = cause.getStackTrace();
        if (iLimited == -1 || iLimited > stackTrace.length)
            iLimited = stackTrace.length;
        StringBuffer sb = new StringBuffer("\n  ");
        int iI = 0;
        for (; iI < iLimited; iI++) {
            sb.append(stackTrace[iI].toString() + "\n  ");
        }
        if (iI < stackTrace.length) {
            sb.append("  ....\n");
        }
        return sb;
    }

    public static void removeCookie(OidcClientRequest oidcClientRequest) {
        String cookieName = oidcClientRequest.getOidcClientCookieName();
        Cookie c = OidcClientUtil.createCookie(cookieName,
                "",
                oidcClientRequest.getRequest());
        c.setMaxAge(0);
        oidcClientRequest.getResponse().addCookie(c);
    }

    // The sujbect has to be non-null
    public static String getUserNameFromSubject(Subject subject) {
        Iterator<Principal> it = subject.getPrincipals().iterator();
        String username = it.next().getName();
        return username;
    }

    /**
     * Encodes the given string using URLEncoder and UTF-8 encoding.
     *
     * @param value
     * @return
     */
    public static String encode(String value) {
        if (value == null) {
            return value;
        }
        try {
            value = URLEncoder.encode(value, Constants.UTF_8);
        } catch (UnsupportedEncodingException e) {
            // Do nothing - UTF-8 should always be supported
        }
        return value;
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
            String rebuiltParam = encode(param);
            int equalIndex = param.indexOf("=");
            if (equalIndex > -1) {
                String name = param.substring(0, equalIndex);
                String value = (equalIndex < (param.length() - 1)) ? param.substring(equalIndex + 1) : "";
                rebuiltParam = encode(name) + "=" + encode(value);
            }
            if (!rebuiltParam.isEmpty()) {
                rebuiltQuery.append(rebuiltParam + "&");
            }
        }
        // Remove trailing '&' character
        if (rebuiltQuery.length() > 0 && rebuiltQuery.charAt(rebuiltQuery.length() - 1) == '&') {
            rebuiltQuery.deleteCharAt(rebuiltQuery.length() - 1);
        }
        return rebuiltQuery.toString();
    }

    /**
     * @param e
     * @return
     */
    public static String getCauseMsg(Throwable e) {
        if (e instanceof NullPointerException) { // NPE is a special case when it happens in open source
            return "[Missing some data. Got a NullPointerException]";
        }
        String exceptionName = e.getClass().getSimpleName();
        String exceptionMsg = e.getMessage();
        String result = "[" + exceptionName + "]";
        if (exceptionMsg != null) {
            return result + "[" + exceptionMsg + "]";
        } else {
            return result;
        }
    }

    static String[] zeroFillers = new String[] {
            "", "0", "00", "000", "0000", "00000", "000000"
    };

    public static String getTimeStamp() {
        long lNumber = (new Date()).getTime();
        return getTimeStamp(lNumber);
    }

    // for unit test
    public static String getTimeStamp(long lNumber) {
        String timeStamp = "" + lNumber;
        int iIndex = TIMESTAMP_LENGTH - timeStamp.length(); // this is enough for 3000 years
        return zeroFillers[iIndex] + timeStamp;
    }

    public static long convertNormalizedTimeStampToLong(String input) {
        // no need to check the input since the caller already check it. see verifyState() in OidcClientAuthenticator
        String timeStamp = input.substring(0, TIMESTAMP_LENGTH);
        long lTimeStamp = Long.parseLong(timeStamp);
        return lTimeStamp;
    }

    /**
     * @param nonceValue
     * @param state
     * @param clientConfig
     */
    @Trivial
    public static void createNonceCookie(OidcClientRequest oidcClientRequest, String nonceValue, String state, ConvergedClientConfig clientConfig) {
        String cookieName = HashUtils.getCookieName(ClientConstants.WAS_OIDC_NONCE, clientConfig, state);
        String cookieValue = createNonceCookieValue(nonceValue, state, clientConfig);
        Cookie cookie = OidcClientUtil.createCookie(cookieName, cookieValue, oidcClientRequest.getRequest());
        oidcClientRequest.getResponse().addCookie(cookie);
    }

    /**
     * @param nonceInIDToken
     * @param clientConfig
     * @param responseState
     * @return
     */
    @Trivial
    public static boolean verifyNonce(OidcClientRequest oidcClientRequest, String nonceInIDToken, ConvergedClientConfig clientConfig, String responseState) {
        String cookieName = HashUtils.getCookieName(ClientConstants.WAS_OIDC_NONCE, clientConfig, responseState);
        String cookieValue = createNonceCookieValue(nonceInIDToken, responseState, clientConfig);
        String oldCookieValue = CookieHelper.getCookieValue(oidcClientRequest.getRequest().getCookies(), cookieName);
        OidcClientUtil.invalidateReferrerURLCookie(oidcClientRequest.getRequest(), oidcClientRequest.getResponse(), ClientConstants.WAS_OIDC_NONCE);
        return cookieValue.equals(oldCookieValue);
    }

    // calculate the cookie value of Nonce
    public static String createNonceCookieValue(String nonceValue, String state, ConvergedClientConfig clientConfig) {
        return HashUtils.digest(nonceValue + state + clientConfig.getClientSecret());
    }

}
