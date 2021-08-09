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
package com.ibm.ws.security.oauth20.web;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.InvalidPasswordEncodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.api.OAuth20Provider;

/**
 *  Class for common functions used when handling web requests
 */
public class EndpointUtils {

    private static TraceComponent tc = Tr.register(EndpointUtils.class, "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");
    public final static String PBKDF2WithHmacSHA512 = "PBKDF2WithHmacSHA512";
    public final static String DEFAULT_HASH = PBKDF2WithHmacSHA512;
    public final static String PLAIN = "plain";
    public final static String HASH = "hash";

    /**
     * Determines if this user hit the token limit for the user / client combination
     *
     * @param provider
     * @param request
     * @return
     */
    public static boolean reachedTokenLimit(OAuth20Provider provider, HttpServletRequest request, String userName, String clientId) {
        long limit = provider.getClientTokenCacheSize(); // this method returns user client token limit. clientTokenCacheSize attribute is not in use.
        if (limit > 0) {
            long numtokens = getTokensForUser(false, true, userName, clientId, provider).size();
            if (numtokens >= limit) {
                Tr.error(tc, "security.oauth20.token.limit.error", new Object[] { userName, clientId, limit });
                return true;
            }
        }
        return false;
    }

    public static String getParameter(HttpServletRequest request, String param) {
        return request.getParameter(param);
    }

    public static Collection<OAuth20Token> getTokensForUser(boolean isAppPasswordRequest, boolean isTokenRequest, String userName, String clientId, OAuth20Provider provider) {
        String decUserName = userName;
        try {
            decUserName = java.net.URLDecoder.decode(userName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // ffdc
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "raw user name: " + userName + " urlDecoded user name going to database: " + decUserName);
        } // disallow refresh and id tokens

        if (isTokenRequest) {
            Collection<OAuth20Token> tokens = provider.getTokenCache().getUserAndClientTokens(decUserName, clientId);
            return removeAppPasswordOrAppTokens(tokens);
        } else {
            String tokenType = isAppPasswordRequest ? OAuth20Constants.APP_PASSWORD_STATE_ID : OAuth20Constants.APP_TOKEN_STATE_ID;
            return provider.getTokenCache().getMatchingTokens(decUserName, clientId, tokenType);
        }
    }

    /**
    * @param tokens
    * @param clientId
    * @return
    */
    private static Collection<OAuth20Token> getTokensMatchingClientId(Collection<OAuth20Token> tokens, String clientId) {
        Iterator<OAuth20Token> it = tokens.iterator();
        HashSet<OAuth20Token> matchingTokens = new HashSet<OAuth20Token>();
        while (it.hasNext()) {
            OAuth20Token token = it.next();
            if (clientId.equals(token.getClientId())) {
                matchingTokens.add(token);
            }
        }
        return matchingTokens;
    }

    /**
     * @param tokens
     * @param grantType
     * @return
     */
    private static Collection<OAuth20Token> getTokensMatchingGrantType(Collection<OAuth20Token> tokens, String grantType) {
        Iterator<OAuth20Token> it = tokens.iterator();
        HashSet<OAuth20Token> matchingTokens = new HashSet<OAuth20Token>();
        while (it.hasNext()) {
            OAuth20Token token = it.next();
            if (token.getGrantType().equals(grantType)) {
                matchingTokens.add(token);
            }
        }
        return matchingTokens;

    }

    /**
     * @param tokens
     * @param grantType
     * @return
     */
    private static Collection<OAuth20Token> removeAppPasswordOrAppTokens(Collection<OAuth20Token> tokens) {
        HashSet<OAuth20Token> matchingTokens = new HashSet<OAuth20Token>();
        if (tokens != null) {
            Iterator<OAuth20Token> it = tokens.iterator();

            String appPasswdGrantType = OAuth20Constants.APP_PASSWORD;
            String appTokenGrantType = OAuth20Constants.APP_TOKEN;
            while (it.hasNext()) {
                OAuth20Token token = it.next();
                if (!(token.getGrantType().equals(appPasswdGrantType)) && !(token.getGrantType().equals(appTokenGrantType))) {
                    matchingTokens.add(token);
                }
            }
        }
        return matchingTokens;
    }

    @FFDCIgnore({ InvalidPasswordEncodingException.class, UnsupportedCryptoAlgorithmException.class })
    public static String computeTokenHash(OAuth20Token entry, @Sensitive String lookupKeyParam, String grantType) {

        String lookupKey = lookupKeyParam;
        String gt = null;// OAuth20Constants.GRANT_TYPE_APP_PASSWORD;
        if (entry != null) {
            gt = entry.getGrantType();
        } else if (grantType != null) {
            gt = grantType;
        }
        if (OAuth20Constants.GRANT_TYPE_APP_PASSWORD.equals(gt) || OAuth20Constants.GRANT_TYPE_APP_TOKEN.equals(gt)) {
            Map<String, String> properties = new HashMap<String, String>();
            properties.put(PasswordUtil.PROPERTY_NO_TRIM, "true");
            properties.put(PasswordUtil.PROPERTY_HASH_SALT, OAuth20Constants.APP_PASSWORD_HASH_SALT);
            try {
                lookupKey = PasswordUtil.encode(lookupKeyParam, OAuth20Constants.HASH, properties);

            } catch (InvalidPasswordEncodingException e) {

            } catch (UnsupportedCryptoAlgorithmException e) {
                // TODO : message

            }
        }
        return lookupKey;
    }

    @Trivial
    public static String computeTokenHash(String lookupKeyParam) {
        return computeTokenHash(lookupKeyParam, null);
    }

    @FFDCIgnore({ InvalidPasswordEncodingException.class, UnsupportedCryptoAlgorithmException.class })
    public static String computeTokenHash(String lookupKeyParam, String algorithm) {
        String lookupKey = lookupKeyParam;
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PasswordUtil.PROPERTY_NO_TRIM, "true");
        properties.put(PasswordUtil.PROPERTY_HASH_SALT, OAuth20Constants.APP_PASSWORD_HASH_SALT);
        properties.put(PasswordUtil.PROPERTY_HASH_ALGORITHM, algorithm == null ? DEFAULT_HASH : algorithm);
        try {
            lookupKey = PasswordUtil.encode(lookupKeyParam, OAuth20Constants.HASH, properties);

        } catch (InvalidPasswordEncodingException e) {
            // TODO : message
            // throw error
        } catch (UnsupportedCryptoAlgorithmException e) {
            // TODO : message
            // throw error
        }

        return lookupKey;
    }

    // fix any unescaped ". Doesn't handle bs,ff,\n,\r,\t \Uxxxx, \\, \/
    public static String escapeQuotesForJson(String in) {
        String quot = "\"";
        String bsh = "\\";
        if (!in.contains(quot)) {
            return in;
        }
        String prev = "";
        String next = "";
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length(); i++) {
            prev = next;
            next = in.substring(i, i + 1);
            if (next.equals(quot)) {
                if (!prev.equals(bsh)) {
                    out.append(bsh).append(next); // fix "
                    continue;
                }
            }
            out.append(next);
        }
        return out.toString();
    }
}
