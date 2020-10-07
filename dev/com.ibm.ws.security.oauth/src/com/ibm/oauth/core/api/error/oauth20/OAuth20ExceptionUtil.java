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
package com.ibm.oauth.core.api.error.oauth20;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.oauth.core.api.error.OAuthException;
import com.ibm.ws.security.oauth20.web.WebUtils;

public class OAuth20ExceptionUtil {

    public static Map<Class<?>, String> exceptionMessage = null;
    private static Map<Locale, ResourceBundle> resourceBundleMap = new HashMap<Locale, ResourceBundle>();
    private static String RESOURCE_BUNDLE = "com.ibm.ws.security.oauth20.resources.ProviderMsgs";
    private static TraceComponent tc = Tr.register(OAuth20ExceptionUtil.class,
            "OAuth20Provider", "com.ibm.ws.security.oauth20.resources.ProviderMsgs");
    private static String UTF8_ENCODING = "utf-8";
    static {
        Map<Class<?>, String> map = new HashMap<Class<?>, String>();
        map.put(OAuth20AccessDeniedException.class, "security.oauth20.error.access.denied");
        map.put(OAuth20AuthorizationCodeInvalidClientException.class, "security.oauth20.error.invalid.authorizationcode");
        map.put(OAuth20BadParameterFormatException.class, "security.oauth20.error.parameter.format");
        map.put(OAuth20DuplicateParameterException.class, "security.oauth20.error.duplicate.parameter");
        map.put(OAuth20InvalidClientException.class, "security.oauth20.error.invalid.client");
        map.put(OAuth20InvalidClientSecretException.class, "security.oauth20.error.invalid.clientsecret");
        map.put(OAuth20InvalidGrantTypeException.class, "security.oauth20.error.invalid.granttype");
        map.put(OAuth20InvalidRedirectUriException.class, "security.oauth20.error.invalid.redirecturi");
        map.put(OAuth20InvalidResponseTypeException.class, "security.oauth20.error.invalid.responsetype");
        map.put(OAuth20InvalidScopeException.class, "security.oauth20.error.invalid.scope");
        map.put(OAuth20InvalidTokenException.class, "security.oauth20.error.invalid.token");
        map.put(OAuth20InvalidTokenRequestMethodException.class, "security.oauth20.error.invalid.tokenrequestmethod");
        map.put(OAuth20MismatchedClientAuthenticationException.class, "security.oauth20.error.mismatched.clientauthentication");
        map.put(OAuth20MismatchedRedirectUriException.class, "security.oauth20.error.mismatched.redirecturi");
        map.put(OAuth20MissingParameterException.class, "security.oauth20.error.missing.parameter");
        map.put(OAuth20PublicClientCredentialsException.class, "security.oauth20.error.publicclient.credential");
        map.put(OAuth20PublicClientForbiddenException.class, "security.oauth20.error.publicclient.forbidden");
        map.put(OAuth20RefreshTokenInvalidClientException.class, "security.oauth20.error.refreshtoken.invalid.client");
        exceptionMessage = Collections.unmodifiableMap(map);
    }

    /**
     * Preserved for compatibility, calls new signature
     *
     * @param e
     * @param locale
     * @return
     */
    public static String getMessage(OAuthException e, Locale locale) {
        return getMessage(e, locale, UTF8_ENCODING);
    }

    /**
     * Get the appropriate error message for the given exception
     *
     * @param e
     * @param locale
     * @param encoding
     * @return
     */
    public static String getMessage(OAuthException e, Locale locale, String encoding) {
        String retVal = e.getMessage();

        if (e instanceof OAuth20AccessDeniedException) {
            OAuth20AccessDeniedException e1 = (OAuth20AccessDeniedException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] {});
        } else if (e instanceof OAuth20AuthorizationCodeInvalidClientException) {
            OAuth20AuthorizationCodeInvalidClientException e1 = (OAuth20AuthorizationCodeInvalidClientException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getClientId(), locale, encoding) });
        } else if (e instanceof OAuth20BadParameterFormatException) {
            OAuth20BadParameterFormatException e1 = (OAuth20BadParameterFormatException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { e1.getParamName(), WebUtils.encode(e1.getParamValue(), locale, encoding) });
        } else if (e instanceof OAuth20DuplicateParameterException) {
            OAuth20DuplicateParameterException e1 = (OAuth20DuplicateParameterException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getParamName(), locale, encoding) });
        } else if (e instanceof OAuth20InvalidClientException) {
            OAuth20InvalidClientException e1 = (OAuth20InvalidClientException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getClientId(), locale, encoding) });
        } else if (e instanceof OAuth20InvalidClientSecretException) {
            OAuth20InvalidClientSecretException e1 = (OAuth20InvalidClientSecretException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getClientId(), locale, encoding) });
        } else if (e instanceof OAuth20InvalidGrantTypeException) {
            OAuth20InvalidGrantTypeException e1 = (OAuth20InvalidGrantTypeException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getGrantType(), locale, encoding) });
        } else if (e instanceof OAuth20InvalidRedirectUriException) {
            OAuth20InvalidRedirectUriException e1 = (OAuth20InvalidRedirectUriException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getRedirectURI(), locale, encoding) });
        } else if (e instanceof OAuth20InvalidResponseTypeException) {
            OAuth20InvalidResponseTypeException e1 = (OAuth20InvalidResponseTypeException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getResponseType(), locale, encoding) });
        } else if (e instanceof OAuth20InvalidScopeException) {
            OAuth20InvalidScopeException e1 = (OAuth20InvalidScopeException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getRequestedScope(), locale, encoding),
                            e1.getApprovedScope() });
        } else if (e instanceof OAuth20InvalidTokenException) {
            OAuth20InvalidTokenException e1 = (OAuth20InvalidTokenException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getKey(), locale, encoding), e1.getTokenType(),
                            e1.getTokenSubType() });
        } else if (e instanceof OAuth20InvalidTokenRequestMethodException) {
            OAuth20InvalidTokenRequestMethodException e1 = (OAuth20InvalidTokenRequestMethodException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { e1.getMethod() });
        } else if (e instanceof OAuth20MismatchedClientAuthenticationException) {
            OAuth20MismatchedClientAuthenticationException e1 = (OAuth20MismatchedClientAuthenticationException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getClientId(), locale, encoding), e1.getAuthenticatedClient() });
        } else if (e instanceof OAuth20MismatchedRedirectUriException) {
            OAuth20MismatchedRedirectUriException e1 = (OAuth20MismatchedRedirectUriException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getReceivedRedirectURI(), locale, encoding),
                            e1.getIssuedToRedirectURI() });
        } else if (e instanceof OAuth20MissingParameterException) {
            OAuth20MissingParameterException e1 = (OAuth20MissingParameterException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { e1.getParam() });
        } else if (e instanceof OAuth20PublicClientCredentialsException) {
            OAuth20PublicClientCredentialsException e1 = (OAuth20PublicClientCredentialsException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getClientId(), locale, encoding) });
        } else if (e instanceof OAuth20PublicClientForbiddenException) {
            OAuth20PublicClientForbiddenException e1 = (OAuth20PublicClientForbiddenException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getClientId(), locale, encoding) });
        } else if (e instanceof OAuth20RefreshTokenInvalidClientException) {
            OAuth20RefreshTokenInvalidClientException e1 = (OAuth20RefreshTokenInvalidClientException) e;
            retVal = MessageFormat.format(getResourceBundle(locale).getString(exceptionMessage.get(e1.getClass())),
                    new Object[] { WebUtils.encode(e1.getRefreshToken(), locale, encoding),
                            WebUtils.encode(e1.getClientId(), locale, encoding) });
        }

        return retVal;
    }

    static ResourceBundle getResourceBundle(Locale locale) {
        ResourceBundle rb = resourceBundleMap.get(locale);
        if (rb == null) {
            rb = ResourceBundle.getBundle(RESOURCE_BUNDLE, locale);
            resourceBundleMap.put(locale, rb);
        }
        return rb;
    }

}
