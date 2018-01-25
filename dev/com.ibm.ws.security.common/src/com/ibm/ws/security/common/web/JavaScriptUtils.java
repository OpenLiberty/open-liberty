/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
        return "document.cookie=\"" + getHtmlCookieString(name, value, cookieProperties) + "\";";
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
        if (name == null || name.isEmpty()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot create a cookie string because the cookie name [" + name + "] was null or empty.");
            }
            return "";
        }
        String cookieString = createHtmlCookiePropertyString(name, value);
        cookieString += createHtmlCookiePropertiesString(cookieProperties);
        return cookieString;
    }

    /**
     * Creates a JavaScript HTML block that:
     * <ol>
     * <li>Creates a cookie with the specified name whose value is the browser's current location
     * <li>Redirects the browser to the specified URL
     * </ol>
     */
    public String getJavaScriptForRedirect(String reqUrlCookieName, String redirectUrl) throws Exception {
        String javascript = createJavaScriptForRedirectCookie(reqUrlCookieName) + "\n";
        javascript += createJavaScriptStringToPerformRedirect(redirectUrl) + "\n";
        return wrapInJavascriptHtmlTags(javascript);
    }

    String createHtmlCookiePropertiesString(Map<String, String> props) {
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

    String createHtmlCookiePropertyString(String name, String value) {
        if (name == null || name.isEmpty()) {
            return "";
        }
        String propertyString = WebUtils.htmlEncode(name);
        if (value != null) {
            propertyString += "=" + WebUtils.htmlEncode(value);
        }
        propertyString += ";";
        return propertyString;
    }

    String wrapInJavascriptHtmlTags(String javascript) {
        return getJavaScriptHtmlTagStart() + "\n" + javascript + "\n" + getJavaScriptHtmlTagEnd();
    }

    String createJavaScriptForRedirectCookie(String cookieName) {
        if (cookieName == null || cookieName.isEmpty()) {
            return "";
        }
        String jsLocationVarName = "loc";
        String result = "var " + jsLocationVarName + "=window.location.href;\n";
        result += "document.cookie=\"" + WebUtils.htmlEncode(cookieName) + "=\"+encodeURI(" + jsLocationVarName + ")+\";";
        result += createHtmlCookiePropertiesString(getCookieProperties());
        result += "\";";
        return result;
    }

    Map<String, String> getCookieProperties() {
        Map<String, String> cookieProperties = new HashMap<String, String>();
        cookieProperties.put("path", "/");
        WebAppSecurityConfig webAppSecurityConfig = getWebAppSecurityConfig();
        if (webAppSecurityConfig != null) {
            if (webAppSecurityConfig.getSSORequiresSSL()) {
                cookieProperties.put("secure", null);
            }
        }
        return cookieProperties;
    }

    String createJavaScriptStringToPerformRedirect(String redirectUrl) throws Exception {
        if (!WebUtils.validateUriFormat(redirectUrl)) {
            throw new Exception(Tr.formatMessage(tc, "JAVASCRIPT_REDIRECT_URL_NOT_VALID", new Object[] { redirectUrl }));
        }
        return "window.location.replace(\"" + redirectUrl + "\");";
    }

    WebAppSecurityConfig getWebAppSecurityConfig() {
        return WebAppSecurityCollaboratorImpl.getGlobalWebAppSecurityConfig();
    }

}
