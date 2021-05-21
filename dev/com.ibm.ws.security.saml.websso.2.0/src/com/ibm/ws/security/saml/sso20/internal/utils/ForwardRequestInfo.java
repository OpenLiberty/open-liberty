/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.web.JavaScriptUtils;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.error.SamlException;


/**
 * To store the request info in the cache
 * the request information will be recalled in order to process after the idp provide a samlResponse
 */
public class ForwardRequestInfo extends HttpRequestInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient static final TraceComponent tc = Tr.register(ForwardRequestInfo.class,
                                                                   TraceConstants.TRACE_GROUP,
                                                                   TraceConstants.MESSAGE_BUNDLE);

    boolean bNeedFragment = true;

    /**
     * 
     * constructor for Unsolicited (get only):
     * 1) relayState which does not have previous stored requestInfo (idpInit)
     * ** This will be called later at redirectCachedRequestNoFragment
     * ** Since this is GET Method, no need to save parameters for POST
     * 2) redirect idpUrl which is defined in the server.xml (not the idpUrl from idpMetadata)
     * ** This will be called later at redirectGetRequest
     * 
     * @param requestUrl
     * @param queryString
     */
    public ForwardRequestInfo(String requestUrl, String queryString) {
        this.method = METHOD_GET;
        // bUnsolicited = true;
        this.reqUrl = requestUrl;
        this.queryString = queryString;
        if (queryString != null && !queryString.isEmpty()) {
            this.requestURL = requestUrl + "?" + queryString;
        } else {
            this.requestURL = requestUrl; // no queryString
        }
        // No need to set up reqUrl, since it's GET only. No restore for POST.
    }

    /**
     * Constructor for post to redirect to idpUrl (Solicited)
     * For post only
     * Later call redirectPostRequest
     * 
     * @param requestUri
     */
    public ForwardRequestInfo(String requestUri) {
        this.method = METHOD_POST;
        // bSolicited = true;
        this.reqUrl = requestUri;
        this.requestURL = requestUri;
    }

    /**
     * Set parameters for Solicited
     * 
     * @param key
     * @param values
     */
    public void setParameter(String key, String[] values) {
        if (this.parameters == null) {
            this.parameters = new HashMap<String, String[]>();
        }
        this.parameters.put(key, values);
    }

    // for FAT
    @Override
    public String getQueryString() {
        return queryString;
    }

    // for FAT
    @Override
    public String getRequestUrl() {
        return this.reqUrl;
    }

    // the redirect
    public void redirectRequest(HttpServletRequest req,
                                HttpServletResponse resp,
                                String cookieName,
                                String cookieValue) throws SamlException {

        try {
            // handling cookie and headers
            if (cookieName != null && cookieValue != null) {
                RequestUtil.createCookie(req,
                                         resp,
                                         cookieName,
                                         cookieValue);
            }

            // HTTP 1.1.
            resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private, max-age=0");
            // HTTP 1.0. 
            resp.setHeader("Pragma", "no-cache");
            // Proxies. 
            resp.setDateHeader("Expires", 0);
            resp.setContentType("text/html");

            if (this.method.equalsIgnoreCase("POST") || (this.parameters != null && !this.parameters.isEmpty())) {
                // create the form content
                StringBuffer sb = new StringBuffer();
                try {
                    sb.append("<HTML xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">");
                    sb.append("<HEAD>");
                    sb.append("</HEAD>");
                    sb.append("<BODY onload=\"document.forms[0].submit()\">");
                    sb.append("<FORM name=\"redirectform\" id=\"redirectform\" action=\"");
                    sb.append(this.reqUrl);
                    if (fragement != null && !fragement.isEmpty()) {
                        sb.append("#" + fragement);
                    }
                    sb.append("\" method=\"" + this.method + "\"><div>");
                    if (this.bNeedFragment) {
                        sb.append(handleFragmentCookies());
                    }
                    if (this.parameters != null && !this.parameters.isEmpty()) {
                        Set<Entry<String, String[]>> set = this.parameters.entrySet();
                        for (Entry<String, String[]> entry : set) {
                            String key = entry.getKey();
                            String[] values = entry.getValue();
                            if (values != null && values.length > 0) {
                                for (int iI = 0; iI < values.length; iI++) {
                                    sb.append("<input type=\"hidden\" name=\"" + key +
                                              "\" value=\"" + values[iI] + "\"/>"); // the value had been encoded in handleParameter()
                                }
                            } else {
                                sb.append("<input type=\"hidden\" name=\"" + key +
                                          " value=\"\"/>");
                            }
                        }
                    }
                    sb.append("</div>");
                    sb.append("<noscript><div>");
                    sb.append("<button type=\"submit\" name=\"redirectform\">Process request</button>");
                    sb.append("</div></noscript>");
                    sb.append("</FORM></BODY></HTML>");
                } catch (Exception e) {
                    // This should not happen
                    throw new SamlException(e); // Let SamlException handle the unexpected Exception
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "... expect to be redirected by the browser (" + this.method + ")\n" +
                                 sb.toString());
                }

                PrintWriter out = resp.getWriter();
                out.println(sb.toString());
                out.flush();
            }
            else {
                String urlGet = this.reqUrl;
                if (fragement != null && !fragement.isEmpty()) {
                    urlGet += ("#" + fragement);
                }
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "... expect to be redirected by the browser (" + this.method + ")\n" +
                                 urlGet);
                }
                resp.sendRedirect(urlGet);
            }

        } catch (IOException e) {
            // Error handling , in case
            throw new SamlException(e); // Let the SamlException handle the unexpected Exception
        }
    }

    /**
     * @return
     */
    String handleFragmentCookies() {

        String cookieName = Constants.COOKIE_NAME_SAML_FRAGMENT + getFragmentCookieId();

        StringBuffer sb = new StringBuffer();
        sb.append("\n<SCRIPT type=\"TEXT/JAVASCRIPT\" language=\"JavaScript\">\n");
        sb.append("document.cookie = '");
        sb.append(cookieName + "=' + encodeURIComponent(window.location.href) + '; Path=/;"); // session cookie
        JavaScriptUtils jsUtils = new JavaScriptUtils();
        String cookieProps = jsUtils.createHtmlCookiePropertiesString(jsUtils.getWebAppSecurityConfigCookieProperties());
        sb.append(cookieProps);
        sb.append("';\n");
        sb.append("</SCRIPT>\n");

        return sb.toString();
    }

    // This is called when postIdp
    // the bNeedFragment by default is true
    public void redirectPostRequest(HttpServletRequest req,
                                    HttpServletResponse response,
                                    String cookieName,
                                    String cookieValue) throws SamlException {
        this.method = METHOD_POST;
        redirectRequest(req,
                        response,
                        cookieName,
                        cookieValue);

    }

    // This is called when the unsolicited is calling the loginPageUrl 
    // the bNeedFragment by default is true
    public void redirectGetRequest(HttpServletRequest req,
                                   HttpServletResponse response,
                                   String cookieName,
                                   String cookieValue,
                                   boolean fragmentOption) throws SamlException {
        this.method = METHOD_GET;
        this.bNeedFragment = fragmentOption;
        if (this.bNeedFragment) {
            queryStringToParameters();
        }
        redirectRequest(req,
                        response,
                        cookieName,
                        cookieValue);
    }

    /**
     * Parse the requeslURL and querySTring into parameters
     * 
     * @throws SamlException
     */
    void queryStringToParameters() throws SamlException {
        if (this.parameters == null) {
            this.parameters = new HashMap<String, String[]>();
        }
        try {
            int index = this.reqUrl.indexOf("?");
            if (index > 0) { // should not be 0, otherwise it is in error
                String urlQuery = this.reqUrl.substring(index + 1);
                this.reqUrl = this.reqUrl.substring(0, index);
                queryStringToParameters(urlQuery);
            }
            queryStringToParameters(this.queryString);
        } catch (UnsupportedEncodingException e) {
            // should not happen, we are using UTF-8 encode
            throw new SamlException(e); // let the SamlException handle the unexpected exception
        }

    }

    /**
     * @param queryString2
     * @throws UnsupportedEncodingException
     */
    void queryStringToParameters(String query) throws UnsupportedEncodingException {
        if (query == null || query.isEmpty())
            return;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int index = pair.indexOf("=");
            if (index > 0) {
                String key = pair.substring(0, index);
                String value = pair.substring(index + 1);
                handleParameter(key, value);
            } else {
                handleParameter(pair, "");
            }
        }
    }

    /**
     * @param key
     * @param value
     * @throws UnsupportedEncodingException
     */
    void handleParameter(String encodedKey, String encodedValue)
                    throws UnsupportedEncodingException {
        String key = URLDecoder.decode(encodedKey, Constants.UTF8);
        String value = URLDecoder.decode(encodedValue, Constants.UTF8);
        String[] values = getStringArray(key, value);
        this.parameters.put(key, values);
    }

    /**
     * @param key
     * @param value
     * @return
     */
    String[] getStringArray(String key, String value) {
        String[] result = getNewArray(this.parameters.get(key));
        result[result.length - 1] = value;
        return result;
    }

    /**
     * @param strings
     * @return
     */
    String[] getNewArray(String[] strings) {
        if (strings == null)
            return new String[1];
        String[] result = new String[strings.length + 1];
        System.arraycopy(strings, 0, result, 0, strings.length);
        return result;
    }

    /**
     * @param str1
     * @return
     */
    HashMap<String, String[]> parseQueryString(String str1) {
        HashMap<String, String[]> map = new HashMap<String, String[]>();
        String[] entries = str1.split("&");
        for (String entry : entries) {
            int index = entry.indexOf("=");
            if (index < 0) {
                map.put(entry, new String[0]);
            } else {
                String key = entry.substring(0, index);
                String value = entry.substring(index + 1);
                map.put(key, new String[] { value });
            }
        }
        return map;
    }

    public static boolean safeCompare(String str1, String str2) {
        if (str1 == null) {
            return str2 == null;
        } else {
            return str1.equals(str2);
        }
    }

    public static boolean safeCompare(int i1, int i2) {
        return i1 == i2;
    }

}
