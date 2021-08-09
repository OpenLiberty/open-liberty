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
package com.ibm.ws.security.openidconnect.server.plugins;

import java.security.MessageDigest;

import javax.servlet.http.Cookie;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.web.WebSecurityHelper;
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.oauth20.util.OIDCConstants;

/**
 * 
 * 
 */
public class OIDCBrowserStateUtil {

    private static final TraceComponent tc = Tr.register(OIDCBrowserStateUtil.class);
    private final static String CHAR_ENCODING = "UTF-8";
    private final static String HASH_ALGORITHM = "SHA-256";

    /**
     * Generate a browser state cookie.
     * when an authenticated subject exists, extracting LTPToken2 cookie and then hash the value.
     * if not, use a predefined string as a value to be hashed.
     * 
     * @param isUnauthenticate if true, generate a browser state which indicates unauthenticate.
     */
    public static String generateOIDCBrowserState(boolean isUnauthenticate) {

        Cookie c = null;
        if (!isUnauthenticate) {
            try {
                c = WebSecurityHelper.getSSOCookieFromSSOToken();
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception obtaining SSOCookie from SSOToken: " + e);
                }
            }
        }
        String cookieString = OIDCConstants.OIDC_BROWSER_STATE_UNAUTHENTICATE;
        if (c != null) {
            cookieString = c.getValue();
        }

        return generateOIDCBrowserState(cookieString);
    }

    /**
     * Generate a hashed value from given string.
     * 
     * @param input A string to be hashed.
     * 
     */
    protected static String generateOIDCBrowserState(String input) {
        return generateOIDCBrowserState(input, CHAR_ENCODING, HASH_ALGORITHM);
    }

    /**
     * Generate a hashed value from given string.
     * 
     * @param input A string to be hashed
     * @param encoding Character Encoding
     * @param algorithm Hash algorithm
     * 
     */
    protected static String generateOIDCBrowserState(String input, String encoding, String algorithm) {
        // get hash
        String output = null;
        try {
            if (input != null) {
                byte[] inputBytes = input.getBytes(encoding);
                MessageDigest md = MessageDigest.getInstance(algorithm);
                byte[] digest = md.digest(inputBytes);
                output = new String(Base64Coder.base64Encode(digest), encoding);
            }
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception generating hash value : " + e);
            }
        }
        return output;
    }

}
