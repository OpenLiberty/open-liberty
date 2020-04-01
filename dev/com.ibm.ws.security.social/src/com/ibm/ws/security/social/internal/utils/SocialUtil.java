/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.common.web.CommonWebConstants;
import com.ibm.ws.security.social.Constants;
import com.ibm.ws.security.social.SocialLoginConfig;
import com.ibm.ws.security.social.TraceConstants;
import com.ibm.ws.security.social.error.SocialLoginException;
import com.ibm.ws.security.social.internal.Oauth2LoginConfigImpl;
import com.ibm.ws.security.social.internal.OkdServiceLoginImpl;

/**
 * Collection of utility methods for String to byte[] conversion.
 */
public class SocialUtil {
    @SuppressWarnings("unused")
    private static final TraceComponent tc = Tr.register(SocialUtil.class,
            TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);

    static final char[] chars = Constants.CHARS;

    static final String[] numberFiller = new String[] { "00000000",
            "0000000",
            "000000",
            "00000",
            "0000",
            "000",
            "00",
            "0",
            "" };

    static final String JCEPROVIDER_IBM = "IBMJCE";

    static final String SECRANDOM_IBM = "IBMSecureRandom";

    static final String SECRANDOM_SHA1PRNG = "SHA1PRNG";

    @Trivial
    public static String hash(String stringToEncrypt) {
        int hashCode = stringToEncrypt.hashCode();
        if (hashCode < 0) {
            hashCode = hashCode * -1;
            return "n" + hashCode;
        } else {
            return "p" + hashCode;
        }
    }

    /**
     * This method will return a <code>String</code> that is compliant with
     * xs:id simple type which is used to declare SAML identifiers for
     * assertions, requests, and responses. The String returned will contain 33
     * chars and will always start with '_', the probability to have a duplicate
     * is 62^32.
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
    public static String generateRandom(int iCharCnt) {
        StringBuffer sb = new StringBuffer();
        // if (prefix != null)
        // sb.append(prefix);
        Random r = getRandom();
        for (int i = 0; i < iCharCnt; i++) {
            sb.append(chars[r.nextInt(chars.length)]);
        }
        return sb.toString();
    }

    @Trivial
    public static String generateRandomNumber() {
        Random r = getRandom();
        int iNum = r.nextInt(100000000);// 0-99999999
        String tmp = "" + iNum;
        return numberFiller[tmp.length()] + tmp;
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

    public static QName cloneQName(QName qName) {
        if (qName == null)
            return null;
        return new QName(qName.getNamespaceURI(), qName.getLocalPart(), qName.getPrefix());
    }

    /**
     * @param subject
     * @return
     */
    public static WSCredential getWSCredential(Subject subject) {
        if (subject == null)
            return null;
        Set<WSCredential> credentials = subject.getPublicCredentials(WSCredential.class);
        return credentials.iterator().next();
    }

    /**
     * @param realmName
     * @param userId
     * @param cRealmName
     * @param cUserId
     * @return
     */
    public static boolean sameUser(String realmName, String userId, String cRealmName, String cUserId) {
        if (realmName == null) {
            if (cRealmName != null)
                return false;
        } else {
            if (!realmName.equals(cRealmName)) {
                return false;
            }
        }
        if (userId == null) {
            // if( cUserId != null) return false;
            return false;
        } else {
            return userId.equals(cUserId);
        }
    }

    /**
     * @param realmName
     * @param userId
     * @param cRealmName
     * @param cUserId
     * @return
     */
    public static boolean sameUser(String userName, String accessId) {
        if (userName == null) {
            return false;
        }

        return userName.equals(accessId);
    }

    public static boolean validateQueryString(String query) {
        if (query == null) {
            return true;
        }
        return Pattern.matches(CommonWebConstants.VALID_URI_QUERY_CHARS + "*", query);
    }

    public static void validateEndpointFormat(final String endpointUrl) throws SocialLoginException {
        validateEndpointFormat(endpointUrl, true);
    }

    public static void validateEndpointFormat(final String endpointUrl, boolean isHttp) throws SocialLoginException {
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            throw new SocialLoginException("NULL_OR_EMPTY_REQUEST_URL", null, new Object[0]);
        }
        try {
            // Verify that the authorization endpoint is a valid URI
            AccessController.doPrivileged(new PrivilegedExceptionAction<URI>() {
                @Override
                public URI run() throws URISyntaxException {
                    return new URI(endpointUrl);
                }
            });
        } catch (PrivilegedActionException e) {
            throw new SocialLoginException("EXCEPTION_INITIALIZING_URL", null, new Object[] { endpointUrl, e.getException().getLocalizedMessage() });
        }
        String httpPrefix = "https?://";
        String prefix = httpPrefix;
        if (isHttp) {
            // Verify that the endpoint starts with "http://" or "https://"
            if (!Pattern.matches(prefix + ".*", endpointUrl)) {
                throw new SocialLoginException("HTTP_URI_DOES_NOT_START_WITH_HTTP", null, new Object[] { endpointUrl });
            }
        } else {
            // Make the http scheme optional
            prefix = "(?:" + httpPrefix + ")?";
        }
        if (!Pattern.matches(prefix + CommonWebConstants.VALID_URI_PATH_CHARS + "+", endpointUrl)) {
            throw new SocialLoginException("URI_CONTAINS_INVALID_CHARS", null, new Object[] { endpointUrl });
        }
    }

    public static void validateEndpointWithQuery(final String endpointUrl) throws SocialLoginException {
        if (endpointUrl == null || !endpointUrl.contains("?")) {
            SocialUtil.validateEndpointFormat(endpointUrl);
            return;
        }
        // URL contains query string - validate each part separately
        String mainUrl = endpointUrl.substring(0, endpointUrl.indexOf("?"));
        SocialUtil.validateEndpointFormat(mainUrl);

        // Validate query string
        String query = endpointUrl.substring(endpointUrl.indexOf("?") + 1);
        if (!SocialUtil.validateQueryString(query)) {
            throw new SocialLoginException("EXCEPTION_INITIALIZING_URL", null, new Object[] { endpointUrl, "" });
        }
    }

    public static boolean isKubeConfig(SocialLoginConfig clientConfig) {
        String userApiType = clientConfig.getUserApiType();
        return (userApiType != null && Oauth2LoginConfigImpl.USER_API_TYPE_KUBE.equals(userApiType));
    }

    public static boolean isOkdConfig(SocialLoginConfig config) {
        return config.getClass().getName().equals(OkdServiceLoginImpl.class.getName());
    }

    public static boolean useAccessTokenFromRequest(SocialLoginConfig clientConfig) {
        return (clientConfig.isAccessTokenRequired() || clientConfig.isAccessTokenSupported());
    }

}
