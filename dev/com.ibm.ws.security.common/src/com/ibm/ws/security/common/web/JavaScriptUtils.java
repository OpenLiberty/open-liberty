/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.web;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.TraceConstants;
import com.ibm.ws.webcontainer.security.WebAppSecurityCollaboratorImpl;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

public class JavaScriptUtils {
    private static final TraceComponent tc = Tr.register(JavaScriptUtils.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    public String getJavaScriptHtmlTagStart() {
        return "<script type=\"text/javascript\" language=\"javascript\">";
    }

    public String getJavaScriptHtmlTagEnd() {
        return "</script>";
    }

    /**
     * Creates and returns a JavaScript line that sets a cookie with the specified name and value. For example, a cookie name
     * of "test" and value of "123" would return {@code document.cookie="test=123;";}. Note: The name and value will be
     * HTML-encoded.
     */
    public String getJavaScriptHtmlCookieString(String name, String value) {
        return getJavaScriptHtmlCookieString(name, value, null);
    }

    /**
     * Creates and returns a JavaScript line that sets a cookie with the specified name, value, and cookie properties. For
     * example, a cookie name of "test", value of "123", and properties "HttpOnly" and "path=/" would return
     * {@code document.cookie="test=123; HttpOnly; path=/;";}. Note: The name, value, and properties will be HTML-encoded.
     */
    public String getJavaScriptHtmlCookieString(String name, String value, Map<String, String> cookieProperties) {
        return createJavaScriptHtmlCookieString(name, value, cookieProperties, true);
    }

    /**
     * Creates and returns a JavaScript line that sets a cookie with the specified name, value, and cookie properties. For
     * example, a cookie name of "test", value of "123", and properties "HttpOnly" and "path=/" would return
     * {@code document.cookie="test=123; HttpOnly; path=/;";}. Note: The specified name and value will not be HTML-encoded.
     */
    public String getUnencodedJavaScriptHtmlCookieString(String name, String value) {
        return getUnencodedJavaScriptHtmlCookieString(name, value, null);
    }

    /**
     * Creates and returns a JavaScript line that sets a cookie with the specified name, value, and cookie properties. For
     * example, a cookie name of "test", value of "123", and properties "HttpOnly" and "path=/" would return
     * {@code document.cookie="test=123; HttpOnly; path=/;";}. Note: The specified properties will be HTML-encoded but the cookie
     * name and value will not.
     */
    public String getUnencodedJavaScriptHtmlCookieString(String name, String value, Map<String, String> cookieProperties) {
        return createJavaScriptHtmlCookieString(name, value, cookieProperties, false);
    }

    /**
     * Creates and returns a JavaScript line for setting a cookie with the specified name and value. Note: The name and value will
     * be HTML-encoded.
     */
    public String getHtmlCookieString(String name, String value) {
        return getHtmlCookieString(name, value, null);
    }

    /**
     * Creates and returns a JavaScript line for setting a cookie with the specified name, value, and cookie properties. Note: The
     * name, value, and properties will be HTML-encoded.
     */
    public String getHtmlCookieString(String name, String value, Map<String, String> cookieProperties) {
        return createHtmlCookieString(name, value, cookieProperties, true);
    }

    /**
     * Creates and returns a JavaScript line for setting a cookie with the specified name, value, and cookie properties. Note: The
     * name and value will not be HTML encoded.
     */
    public String getUnencodedHtmlCookieString(String name, String value) {
        return getUnencodedHtmlCookieString(name, value, null);
    }

    /**
     * Creates and returns a JavaScript line for setting a cookie with the specified name, value, and cookie properties. Note: The
     * properties will be HTML-encoded but the name and value will not be.
     */
    public String getUnencodedHtmlCookieString(String name, String value, Map<String, String> cookieProperties) {
        return createHtmlCookieString(name, value, cookieProperties, false);
    }

    /**
     * Creates a JavaScript HTML block that:
     * <ol>
     * <li>Creates a cookie with the specified name whose value is the browser's current location
     * <li>Redirects the browser to the specified URL
     * </ol>
     */
    public String getJavaScriptForRedirect(String reqUrlCookieName, String redirectUrl) throws Exception {
        return getJavaScriptForRedirect(reqUrlCookieName, redirectUrl, null);
    }

    /**
     * Creates a JavaScript HTML block that:
     * <ol>
     * <li>Creates a cookie with the specified name whose value is the browser's current location
     * <li>Redirects the browser to the specified URL
     * </ol>
     */
    public String getJavaScriptForRedirect(String reqUrlCookieName, String redirectUrl, Map<String, String> cookieProperties) throws Exception {
        String javascript = createJavaScriptForRedirectCookie(reqUrlCookieName, cookieProperties) + "\n";
        javascript += createJavaScriptStringToPerformRedirect(redirectUrl) + "\n";
        return wrapInJavascriptHtmlTags(javascript);
    }

    public String createJavaScriptHtmlCookieString(String name, String value, Map<String, String> cookieProperties, boolean htmlEncodeNameAndValue) {
        String result = "document.cookie=\"";
        if (htmlEncodeNameAndValue) {
            result += getHtmlCookieString(name, value, cookieProperties);
        } else {
            result += getUnencodedHtmlCookieString(name, value, cookieProperties);
        }
        result += "\";";
        return result;
    }

    private String createHtmlCookieString(String name, String value, Map<String, String> cookieProperties, boolean htmlEncodeNameAndValue) {
        if (name == null || name.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot create a cookie string because the cookie name [" + name + "] was null or empty.");
            }
            return "";
        }
        String cookieString = createHtmlCookiePropertyString(name, value, htmlEncodeNameAndValue);
        if (cookieProperties == null) {
            cookieProperties = new HashMap<String, String>();
        }
        cookieProperties.putAll(getWebAppSecurityConfigCookieProperties());
        cookieString += createHtmlCookiePropertiesString(cookieProperties);
        return cookieString;
    }

    public String createHtmlCookiePropertiesString(Map<String, String> props) {
        String propertyString = "";
        if (props == null) {
            return propertyString;
        }
        for (Entry<String, String> property : props.entrySet()) {
            String propString = createHtmlCookiePropertyString(property.getKey(), property.getValue());
            if (propString == null || propString.isEmpty()) {
                continue;
            }
            propertyString += " " + propString;
        }
        return propertyString;
    }

    private String createHtmlCookiePropertyString(String name, String value) {
        return createHtmlCookiePropertyString(name, value, true);
    }

    private String createHtmlCookiePropertyString(String name, String value, boolean htmlEncodeNameAndValue) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String propertyString = (htmlEncodeNameAndValue ? WebUtils.htmlEncode(name) : name);
        if (value != null) {
            propertyString += "=" + (htmlEncodeNameAndValue ? WebUtils.htmlEncode(value) : value);
        }
        propertyString += ";";
        return propertyString;
    }

    private String wrapInJavascriptHtmlTags(String javascript) {
        return getJavaScriptHtmlTagStart() + "\n" + javascript + "\n" + getJavaScriptHtmlTagEnd();
    }

    private String createJavaScriptForRedirectCookie(String cookieName, Map<String, String> cookieProperties) {
        if (cookieName == null || cookieName.isEmpty()) {
            return "";
        }
        String jsLocationVarName = "loc";
        String result = "var " + jsLocationVarName + "=window.location.href;\n";
        result += "document.cookie=\"" + createHtmlCookiePropertyString(WebUtils.htmlEncode(cookieName), "\"+encodeURI(" + jsLocationVarName + ")+\"", false);
        if (cookieProperties == null) {
            result += createHtmlCookiePropertiesString(getDefaultCookieProperties());
        } else {
            cookieProperties = addWebAppSecConfigPropsToCookieProperties(cookieProperties);
            result += createHtmlCookiePropertiesString(cookieProperties);
        }
        result += "\";";
        return result;
    }

    public Map<String, String> getDefaultCookieProperties() {
        Map<String, String> cookieProperties = new HashMap<String, String>();
        cookieProperties.put("path", "/");
        cookieProperties.putAll(getWebAppSecurityConfigCookieProperties());
        return cookieProperties;
    }

    public Map<String, String> getWebAppSecurityConfigCookieProperties() {
        return getWebAppSecurityConfigCookieProperties(getWebAppSecurityConfig());
    }

    public Map<String, String> getWebAppSecurityConfigCookieProperties(WebAppSecurityConfig webAppSecurityConfig) {
        Map<String, String> cookieProperties = new HashMap<String, String>();
        if (webAppSecurityConfig != null) {
            if (webAppSecurityConfig.getSSORequiresSSL()) {
                cookieProperties.put("secure", null);
            }
            String sameSite = webAppSecurityConfig.getSameSiteCookie();
            if (sameSite != null && !"Disabled".equalsIgnoreCase(sameSite)) {
                cookieProperties.put("SameSite", sameSite);
                if ("None".equalsIgnoreCase(sameSite)) {
                    cookieProperties.put("secure", null);
                }
            }
        }
        return cookieProperties;
    }

    private Map<String, String> addWebAppSecConfigPropsToCookieProperties(Map<String, String> cookieProperties) {
        if (cookieProperties == null) {
            cookieProperties = new HashMap<String, String>();
        }
        Map<String, String> webAppSecProps = getWebAppSecurityConfigCookieProperties();
        for (Entry<String, String> entry : webAppSecProps.entrySet()) {
            if (!cookieProperties.containsKey(entry.getKey())) {
                cookieProperties.put(entry.getKey(), entry.getValue());
            }
        }
        return cookieProperties;
    }

    private String createJavaScriptStringToPerformRedirect(String redirectUrl) throws Exception {
        if (!WebUtils.validateUriFormat(redirectUrl)) {
            throw new Exception(Tr.formatMessage(tc, "JAVASCRIPT_REDIRECT_URL_NOT_VALID", new Object[] { redirectUrl }));
        }
        return "window.location.replace(\"" + WebUtils.htmlEncode(redirectUrl) + "\");";
    }

    WebAppSecurityConfig getWebAppSecurityConfig() {
        return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
    }

}
