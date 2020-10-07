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
package com.ibm.oauth.core.internal.oauth20;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.oauth.core.api.attributes.AttributeList;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException;
import com.ibm.oauth.core.internal.OAuthUtil;

/**
 * Utility functions
 * 
 */
public class OAuth20Util extends OAuthUtil {

    final static String CLASS = OAuth20Util.class.getName();
    static Logger _log = Logger.getLogger(CLASS);
    static Pattern _integerPattern = Pattern.compile("^[0-9]+$");

    // Scope chars can be: %x21 / %x23-5B / %x5D-7E
    static Pattern _scopePattern = Pattern.compile("^[\\!\\#-\\[\\]-\\~]+$");

    // response attributes that should be JSON encoded as numbers
    private static final String[] NUMBER_RESPONSE_FIELDS = new String[] { OAuth20Constants.EXPIRES_IN };

    private static final Set NUMBER_RESPONSE_FIELDS_SET = new HashSet(Arrays
            .asList(NUMBER_RESPONSE_FIELDS));

    /**
     * Compares two scope strings to make sure a requested scope is less then or
     * equal to the approved scope, OAuth 2.0 scope strings are space delimited
     * and order is not important
     * 
     * @param approvedScope
     * @param requestedScope
     * @return
     */
    public static boolean scopeEquals(String[] requestedScope,
            String[] approvedScope) {
        boolean result = false;

        if (approvedScope == null)
            approvedScope = new String[] {};
        if (requestedScope == null)
            requestedScope = new String[] {};

        if (requestedScope.length == 0)
            result = true;

        if (!result) {
            Set<String> approvedSet = new HashSet<String>(Arrays
                    .asList(approvedScope));
            Set<String> requestedSet = new HashSet<String>(Arrays
                    .asList(requestedScope));

            if (approvedSet.containsAll(requestedSet)) {
                result = true;
            }
        }

        return result;
    }

    public static boolean validateRedirectUri(String redirectUri) {
        String methodName = "validateRedirectUri";
        _log.entering(CLASS, methodName, new Object[] { redirectUri });
        boolean finestLoggable = _log.isLoggable(Level.FINEST);

        boolean result = false;

        try {

            // first check if its a valid absolute URI
            if (validateUri(redirectUri)) {
                // next make sure it doesn't include a fragment
                // http://tools.ietf.org/html/draft-ietf-oauth-v2#section-2.1.1
                if (redirectUri.indexOf("#") == -1) {
                    result = true;
                } else {
                    if (finestLoggable) {
                        _log.logp(Level.FINEST, CLASS, methodName,
                                "The redirect URI contains a fragment");
                    }
                }
            } else {
                if (finestLoggable) {
                    _log.logp(Level.FINEST, CLASS, methodName,
                            "The redirect URI is not a valid absolute URI");
                }
            }
        } finally {
            _log.exiting(CLASS, methodName, "" + result);
        }

        return result;
    }

    public static void validateRequiredAttribute(String name, String value)
            throws OAuthException {
        if (value == null || value.length() == 0) {
            throw new OAuth20MissingParameterException("security.oauth20.error.missing.parameter", name, null);
        }
    }

    /**
     * Converts a name value pair string into a JSON encoded string
     * 
     * @param s
     * @return
     */
    public static String JSONEncode(String s) {
        StringBuffer result = new StringBuffer();
        result.append("{");

        if (s != null) {
            String[] params = s.split("&");
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    String[] nameval = params[i].split("=", 2);
                    if (nameval != null
                            && (nameval.length == 1 || nameval.length == 2)) {
                        String name = nameval[0];

                        if (name != null) {
                            String value = "";
                            if (nameval.length == 2) {
                                value = nameval[1];
                            }

                            result.append("\"" + name + "\"");
                            result.append(":");

                            // JSON integers are not wrapped in quotes
                            boolean encodeAsNumber = false;
                            if (NUMBER_RESPONSE_FIELDS_SET.contains(name)) {
                                Matcher m = _integerPattern.matcher(value);
                                encodeAsNumber = m.matches();
                            }
                            if (encodeAsNumber) {
                                result.append(value);
                            } else {
                                result.append("\"" + value + "\"");
                            }

                            if (i < params.length - 1) {
                                result.append(",");
                            }
                        }
                    }
                }
            }
        }

        result.append("}");
        return result.toString();
    }

    public static boolean validateScopeString(String scope) {
        String methodName = "validateScopeString";
        _log.entering(CLASS, methodName, new Object[] { scope });
        boolean result = false;

        try {
            Matcher m = _scopePattern.matcher(scope);
            result = m.matches();
        } finally {
            _log.exiting(CLASS, methodName, "" + result);
        }

        return result;
    }

    public static void populateJwtAccessTokenData(AttributeList attributeList, Map<String, String[]> accessTokenMap) {
        _log.entering(CLASS, "populateJwtAccessTokenData", new Object[] { attributeList, accessTokenMap });
        String issuerIdentifier = attributeList.getAttributeValueByName("issuerIdentifier");
        accessTokenMap.put("issuerIdentifier", new String[] { issuerIdentifier });
        String sharedKey = attributeList.getAttributeValueByNameAndType(OAuth20Constants.CLIENT_SECRET,
                OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
        accessTokenMap.put(OAuth20Constants.CLIENT_SECRET, new String[] { sharedKey });
        String[] audiences = attributeList.getAttributeValuesByNameAndType(OAuth20Constants.RESOURCE,
                OAuth20Constants.ATTRTYPE_PARAM_OAUTH);
        accessTokenMap.put(OAuth20Constants.RESOURCE, audiences);
    }
}
