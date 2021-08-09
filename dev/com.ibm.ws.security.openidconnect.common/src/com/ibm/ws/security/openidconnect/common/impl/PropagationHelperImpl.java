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
/**
 * @version 1.0.0
 */
package com.ibm.ws.security.openidconnect.common.impl;

import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.security.WSSecurityException;
import com.ibm.websphere.security.auth.WSSubject;
import com.ibm.websphere.security.openidconnect.token.IdToken;
import com.ibm.ws.security.openidconnect.common.Constants;
import com.ibm.ws.security.openidconnect.common.TraceConstants;

public class PropagationHelperImpl {
    private static final TraceComponent tc = Tr.register(PropagationHelperImpl.class, TraceConstants.TRACE_GROUP,
            TraceConstants.MESSAGE_BUNDLE);
    static final String keyExpiresIn = "expires_in";

    /**
     * Get the type of access token which the runAsSubject authenticated
     *
     * @return the Type of Token, such as: Bearer
     */
    public static String getAccessTokenType() {
        return getSubjectAttributeString("token_type", true);
    }

    /**
     * @return seconds
     */
    public static long getAccessTokenExpirationTime() {
        long exp = 0l;
        Subject runAsSubject = getRunAsSubject();
        Object objExpiresIn = getSubjectAttributeObject(runAsSubject, keyExpiresIn, true);
        if (objExpiresIn == null) {
            //  The access_token is stored during RS processing since it does not have the exires_in attribute.
            //  Customers is supposed to get the acees_token from RP.
            //  Getting the access_token from RS is not appropriate
            //  return 0L to make it expired already
            return 0l;
        }

        Object objStoreMilliseconds = getSubjectAttributeObject(runAsSubject, Constants.CREDENTIAL_STORING_TIME_MILLISECONDS, true);
        if (objStoreMilliseconds != null) {
            exp = getLong(objStoreMilliseconds) / 1000 +
                    getLong(objExpiresIn); // seconds
        }

        return exp; //
    }

    public static String getAccessToken() {
        return getSubjectAttributeString("access_token", true);
    }

    public static String getScopes() {
        return getSubjectAttributeString("scope", true);
    }

    public static IdToken getIdToken() {
        IdToken idToken = null;
        Subject runAsSubject = getRunAsSubject();
        if (runAsSubject != null) {
            Set<IdToken> idTokens = runAsSubject.getPublicCredentials(IdToken.class);
            for (IdToken idTokenTmp : idTokens) {
                idToken = idTokenTmp;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "public IdToken:" + idToken);
                }
                break;
            }
            if (idToken == null) {
                Set<IdToken> privateIdTokens = runAsSubject.getPrivateCredentials(IdToken.class);
                for (IdToken idTokenTmp : privateIdTokens) {
                    idToken = idTokenTmp;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "private IdToken:" + idToken);
                    }
                    break;
                }
            }
        }
        return idToken;
    }

    static Subject getRunAsSubject() {
        try {
            return WSSubject.getRunAsSubject();
        } catch (WSSecurityException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception while getting runAsSubject:", e.getCause());
            }
            // OIDC_FAILED_RUN_AS_SUBJCET=CWWKS1772W: An exception occurred while attempting to get RunAsSubject. The exception was: [{0}]
            Tr.warning(tc, "OIDC_FAILED_RUN_AS_SUBJCET", e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * @param string
     * @return
     */
    static String getSubjectAttributeString(String attribKey, boolean bindWithAccessToken) {
        Subject runAsSubject = getRunAsSubject();
        if (runAsSubject != null) {
            Object obj = getSubjectAttributeObject(runAsSubject, attribKey, bindWithAccessToken);
            if (obj != null)
                return obj.toString();
        }
        return null;
    }

    /**
     * @param runAsSubject
     * @param attribKey
     * @return object
     */
    static Object getSubjectAttributeObject(Subject subject, String attribKey, boolean bindWithAccessToken) {
        Set<Object> publicCredentials = subject.getPublicCredentials();
        int iCnt = 0;
        for (Object credentialObj : publicCredentials) {
            iCnt++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "publicCredential(" + iCnt + ") class:" + credentialObj.getClass().getName());
            }
            if (credentialObj instanceof Map) {
                if (bindWithAccessToken) {
                    Object accessToken = ((Map<?, ?>) credentialObj).get("access_token");
                    if (accessToken == null)
                        continue; // on credentialObj
                }
                Object value = ((Map<?, ?>) credentialObj).get(attribKey);
                if (value != null)
                    return value;
            }
        }
        Set<Object> privCredentials = subject.getPrivateCredentials();
        for (Object credentialObj : privCredentials) {
            iCnt++;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "privateCredential(" + iCnt + ") class:" + credentialObj.getClass().getName());
            }
            if (credentialObj instanceof Map) {
                if (bindWithAccessToken) {
                    Object accessToken = ((Map<?, ?>) credentialObj).get("access_token");
                    if (accessToken == null)
                        continue; // on credentialObj
                }
                Object value = ((Map<?, ?>) credentialObj).get(attribKey);
                if (value != null)
                    return value;
            }
        }
        return null;
    }

    /**
     * @param string
     * @return
     */
    static long getSubjectAttributeLong(Subject runAsSubject, String attribKey, boolean bindWithAccessToken) {
        if (runAsSubject != null) {
            Object obj = getSubjectAttributeObject(runAsSubject, attribKey, bindWithAccessToken);
            if (obj != null)
                return getLong(obj);
        }
        return 0l;
    }

    public static String getUserInfo() {
        Subject runAsSubject = getRunAsSubject();
        return runAsSubject == null ? null : (String) getSubjectAttributeObject(runAsSubject,
                com.ibm.ws.security.openidconnect.common.Constants.USERINFO_STR,
                false);
    }

    /**
     * @param value
     * @return
     */
    static long getLong(Object value) {
        // null has been checked
        if (value instanceof Long) {
            return ((Long) value).longValue();
        }
        if (value instanceof String) {
            // let's parse it
            try {
                Long lRet = Long.valueOf((String) value);
                return lRet.longValue();
            } catch (NumberFormatException e) {
                // TODO Error handling
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "NumberFormatException on:" + value);
                }
            }
        }
        return 0;
    }

}
