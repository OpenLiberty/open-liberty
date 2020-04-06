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
package com.ibm.oauth.core.internal.oauth20.token;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.oauth.core.api.attributes.Attribute;
import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;

/**
 * Contains some helper functions for dealing with expiry times of tokens
 * 
 */
public class OAuth20TokenHelper {

    final static String CLASS = OAuth20TokenHelper.class.getName();
    static Logger _log = Logger.getLogger(CLASS);

    final static String SDF_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    final static String TZ_UTC = "UTC";

    static boolean finestLoggable = _log.isLoggable(Level.FINEST);

    /**
     * @return the number of seconds until the given token expires
     */
    public static String expiresInSeconds(OAuth20Token token) {
        long expiresAt = token.getCreatedAt()
                + (token.getLifetimeSeconds() * 1000L);
        long now = System.currentTimeMillis();
        long expiresIn = expiresAt - now;

        if (expiresIn < 0)
            expiresIn = 0;

        return "" + (expiresIn / 1000);
    }

    /**
     * @return the UTC time that the given token expires
     */
    public static String expiresUTC(OAuth20Token token) {
        long expires = token.getCreatedAt()
                + (token.getLifetimeSeconds() * 1000L);

        SimpleDateFormat sdf = new SimpleDateFormat(SDF_FORMAT);
        sdf.setLenient(false);
        sdf.setTimeZone(TimeZone.getTimeZone(TZ_UTC));
        return sdf.format(new Date(expires));
    }

    public static boolean isTokenExpired(OAuth20Token token) {
        String methodName = "isTokenExpired";
        _log.entering(CLASS, methodName);
        boolean result = false;

        try {
            long expiresAt = token.getCreatedAt()
                    + (token.getLifetimeSeconds() * 1000L);
            long now = System.currentTimeMillis();
            long expiresIn = expiresAt - now;
            result = (expiresIn <= 0);
        } finally {
            _log.exiting(CLASS, methodName, "" + result);
        }

        return result;

    }

    /**
     * @param map
     * @return
     */

    public static Map<String, String[]> getExternalClaims(AttributeList attrList) {
        Map<String, String[]> result = new HashMap<String, String[]>();
        Attribute[] attributes = attrList.getAttributesByType(OAuth20Constants.EXTERNAL_CLAIMS);
        if (attributes != null && attributes.length > 0) {
            for (Attribute attribute : attributes) {
                result.put(attribute.getName(), attribute.getValuesArray());
            }
        }
        return result.size() > 0 ? result : null;
    }

    /**
     * @param map
     * @return
     */

    public static Map<String, String[]> getExternalClaims(Map<String, String[]> tokenMap, AttributeList attrList) {
        Map<String, String[]> result = tokenMap;
        Attribute[] attributes = attrList.getAttributesByType(OAuth20Constants.EXTERNAL_CLAIMS);
        if (attributes != null && attributes.length > 0) {
            for (Attribute attribute : attributes) {
                result.put(attribute.getName(), attribute.getValuesArray());
            }
        }
        return result;
    }

    /**
     * @param tokenMap
     * @param token
     */
    public static void addExternalClaims(Map<String, String[]> tokenMap, OAuth20Token token) {
        if (finestLoggable) {
            _log.logp(Level.FINEST, CLASS, "addExternalClaims(TokenMap)", "addExternalClaims token:" + token);
        }
        if (token != null) {
            Map<String, String[]> map = token.getExtensionProperties();
            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, "addExternalClaims()", "addExternalClaims map:" + map);
            }
            if (map != null) {
                Set<Map.Entry<String, String[]>> entries = map.entrySet();
                for (Map.Entry<String, String[]> entry : entries) {
                    tokenMap.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * @param tokenMap
     * @param token
     */
    public static boolean addExternalClaims(AttributeList attrList, OAuth20Token token) {
        if (finestLoggable) {
            _log.logp(Level.FINEST, CLASS, "addExternalClaims(attributeList)", "addExternalNames token:" + token);
        }
        boolean bResult = false;
        if (token != null) {
            Map<String, String[]> map = token.getExtensionProperties();
            if (map != null) {
                Set<Map.Entry<String, String[]>> entries = map.entrySet();
                for (Map.Entry<String, String[]> entry : entries) {
                    attrList.setAttribute(entry.getKey(), OAuth20Constants.EXTERNAL_CLAIMS, entry.getValue());
                    bResult = true;
                }
            }
        }
        return bResult;
    }

    /**
     * @param map
     * @return
     */
    public static Map<String, String[]> getExternalClaims(Map<String, String[]> tokenMap) {
        Map<String, String[]> result = new HashMap<String, String[]>();
        Set<Map.Entry<String, String[]>> entries = tokenMap.entrySet();
        for (Map.Entry<String, String[]> entry : entries) {
            String key = entry.getKey();
            if (key.startsWith(OAuth20Constants.EXTERNAL_CLAIMS_PREFIX)) {
                result.put(key, entry.getValue());
            }
        }
        return result.size() > 0 ? result : null;
    }

}
