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
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.Constants;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.error.SamlException;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.servlet.IServletRequest;

/**
 * To store the request info in the cache
 * the request information will be recalled in order to process after the idp provide a samlResponse
 */
public class HttpRequestInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private transient static final TraceComponent tc = Tr.register(HttpRequestInfo.class,
                                                                   TraceConstants.TRACE_GROUP,
                                                                   TraceConstants.MESSAGE_BUNDLE);
    static public final String COOKIE_NAME_SAVED_PARAMS = "WASSamlParams_";

    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";

    // cache
    static final Cache postCache = new Cache(0, 0); // one cache for all. 5 minutes lifetime by default

    String requestURL; // The requestURL with query string
    String requestURLWithFragments = null; // The requestURL with query string and/or fragment. In post method, it may not have queries
    String queryString; // queries
    String reqUrl; // the pure requestURL without queries or fragments. Can be compared when restore
    String method;
    //boolean bFromRequest = false;
    //boolean bSolicited = false;
    //boolean bUnsolicited = false;
    HashMap<String, String[]> parameters;
    String strInResponseToId;
    String fragement = null;
    String fragmentCookieId = null;
    String formLogoutExitPage = null;
    DateTime birthTime = new DateTime();
    @SuppressWarnings("rawtypes")
    Map savedPostParams = null;

    // same package can access to it
    HttpRequestInfo() {};

    /**
     * Constructor for any incoming http request on
     * 1) Solicited or unsolicited(not idpInit)
     * 2) idpInit will not call here.
     * ** This will be called later at redirectCachedRequestNoFragment(redirectToRelayState)
     *
     * @param request
     * @throws SamlException
     */
    public HttpRequestInfo(HttpServletRequest request) throws SamlException {
        this.reqUrl = request.getRequestURL().toString(); // keep this for restore the saved parameters (on SAMLReqponseTai)
        this.requestURL = getRequestURL(request); // save the requestURL and queries if any
        // bFromRequest = true;
        this.method = request.getMethod();
        this.strInResponseToId = SamlUtil.generateRandomID();
        // if FormLogoutExtensionProcessor has specified a logout page, save it off. Stream has already been read.
        formLogoutExitPage = (String) request.getAttribute("FormLogoutExitPage");
        if (METHOD_POST.equalsIgnoreCase(this.method) && formLogoutExitPage == null) {
            // let's save the parameters for restore
            IServletRequest extRequest = (IServletRequest) request;
            try {
                this.savedPostParams = extRequest.getInputStreamData();
            } catch (IOException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "An exception getting InputStreamData : ", new Object[] { e });
                }
                // this should not happen. Let the SamlException handles it
                throw new SamlException(e);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Request: method (" + this.method + ") savedParams:" + this.savedPostParams);
        }
    }
    
    public HttpRequestInfo(String reqUrl, String requestURL, String method, String strInResponseToId, String formlogout, HashMap postParams) {
        this.reqUrl = reqUrl;
        this.requestURL = requestURL;
        this.method = method;
        this.strInResponseToId = strInResponseToId;
        this.formLogoutExitPage = formlogout;
        
        if (METHOD_POST.equalsIgnoreCase(this.method) && formLogoutExitPage == null) {  
                this.savedPostParams = postParams;                
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Request: method (" + this.method + ") savedParams:" + this.savedPostParams);
        }
    }

    public String getFormLogoutExitPage() {
        return this.formLogoutExitPage;
    }

    public String getInResponseToId() {
        return this.strInResponseToId;
    }

    /**
     *
     * constructor for Unsolicited (get only):
     * relayState which does not have previous stored requestInfo (idpInit)
     * ** This will be called later at redirectCachedRequestNoFragment
     * ** Since this is GET Method, no need to save parameters for POST
     *
     * @param requestUrl
     * @param queryString
     */
    public HttpRequestInfo(String requestUrl, String queryString) {
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

    // for FAT
    public String getQueryString() {
        return queryString;
    }

    // for FAT
    public String getReqUrl() {
        return this.reqUrl;
    }
    
    public String getRequestUrl() {
        return this.requestURL;
    }
    
    public Map getSavedPostParams() {
        return this.savedPostParams;
    }

    public String getFragmentCookieId() {
        if (this.fragmentCookieId == null) {
            this.fragmentCookieId = SamlUtil.generateRandom(8);
        }
        return this.fragmentCookieId;
    }

    public void setFragmentCookieId(String fragmentCookieId) {
        this.fragmentCookieId = fragmentCookieId;
    }

    /**
     * called by the action of redirecting to the application(redirectToRelayStae for solicited and unsolicited)
     *
     * @param request
     * @param response
     * @param cookieName --
     * @param cookieValue --
     * @throws SamlException
     */
    public void redirectCachedHttpRequest(HttpServletRequest request, HttpServletResponse response, String cookieName, String cookieValue) throws SamlException {
        // handling cookie and headers
        if (cookieName != null && cookieValue != null) {
            RequestUtil.createCookie(request,
                                     response,
                                     cookieName,
                                     cookieValue);
        }
        // if post, create a cookie to save this instance to the cache
        if (METHOD_POST.equalsIgnoreCase(this.method)) {
            // cookie name is:
            //     Constants.COOKIE_NAME_WAS_SAML_ACS + SamlUtil.hash(providerName)
            String savePostId = SamlUtil.generateRandom(12);
            String cacheKey = SamlUtil.hash(savePostId);
            String postCookieName = getPostCookieName(cookieName);
            RequestUtil.createCookie(request,
                                     response,
                                     postCookieName,
                                     savePostId); // this will also addCookie to the response
            postCache.put(cacheKey, this);
        }
        // redirect with requestURLWithFragments then requestURL
        String redirectUrl = this.requestURLWithFragments == null || this.requestURLWithFragments.isEmpty() ? this.requestURL : this.requestURLWithFragments;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "... expect sendRedirect to '" + redirectUrl + "'");
        }
        try {
            response.sendRedirect(redirectUrl);
        } catch (IOException e) {
            // let SamlException handle the IOException
            throw new SamlException(e);
        }
    }

    /**
     * Prepare for redirectToRelayState (before redirect to application)
     *
     * @param request
     * @param response
     * @throws SamlException
     */
    public void setWithFragmentUrl(HttpServletRequest request,
                                   HttpServletResponse response) throws SamlException {

        String encodedUrlCookieName = Constants.COOKIE_NAME_SAML_FRAGMENT + this.fragmentCookieId;
        String encodedUrl = RequestUtil.getCookieId((IExtendedRequest) request,
                                                    response,
                                                    encodedUrlCookieName);
 
        if (encodedUrlCookieName != null) {
            RequestUtil.removeCookie(request, response, encodedUrlCookieName); // removing WASSamlReq_* cookie
        }

        // encodedURL is encoded since we called encodeURIComponent to get the cookie
        try {
            if (encodedUrl != null && !encodedUrl.isEmpty())
                this.requestURLWithFragments = URLDecoder.decode(encodedUrl, Constants.UTF8);
        } catch (UnsupportedEncodingException e) {
            // this should not happen since we are calling with UTF8;
            throw new SamlException(e);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Original RequestUrl:" + this.reqUrl + "\n  requestURLWithFragments:" + this.requestURLWithFragments);
        }
    }

    /**
     * @param cookieName
     * @return
     */
    String getPostCookieName(String cookieName) {
        // cookie name is:
        //     Constants.COOKIE_NAME_WAS_SAML_ACS + SamlUtil.hash(providerName)
        String hashProviderId = cookieName.substring(Constants.COOKIE_NAME_WAS_SAML_ACS.length());
        return COOKIE_NAME_SAVED_PARAMS + hashProviderId;
    }

    /**
     * @return the birthTime
     */
    public DateTime getBirthTime() {
        return birthTime;
    }

    static public String getRequestURL(HttpServletRequest req) {
        StringBuffer reqURL = req.getRequestURL();
        String queryString = req.getQueryString();
        if (queryString != null) {
            reqURL.append("?");
            reqURL.append(encodeQuery(queryString));
        }
        return reqURL.toString();
    }

    /**
     * Encodes each parameter in the provided query. Expects the query argument to be the query string of a URL with parameters
     * in the format: param=value(&param2=value2)*
     *
     * @param query
     * @return
     */
    public static String encodeQuery(String query) {
        if (query == null) {
            return null;
        }

        StringBuilder rebuiltQuery = new StringBuilder();

        // Encode parameters to mitigate XSS attacks
        String[] queryParams = query.split("&");
        for (String param : queryParams) {
            String rebuiltParam = encode(param);
            int equalIndex = param.indexOf("=");
            if (equalIndex > -1) {
                String name = param.substring(0, equalIndex);
                String value = (equalIndex < (param.length() - 1)) ? param.substring(equalIndex + 1) : "";
                rebuiltParam = encode(name) + "=" + encode(value);
            }
            if (!rebuiltParam.isEmpty()) {
                rebuiltQuery.append(rebuiltParam + "&");
            }
        }
        // Remove trailing '&' character
        if (rebuiltQuery.length() > 0 && rebuiltQuery.charAt(rebuiltQuery.length() - 1) == '&') {
            rebuiltQuery.deleteCharAt(rebuiltQuery.length() - 1);
        }
        return rebuiltQuery.toString();
    }

    /**
     * Encodes the given string using URLEncoder and UTF-8 encoding.
     *
     * @param value
     * @return
     */
    public static String encode(String value) {
        if (value == null) {
            return value;
        }
        try {
            value = URLEncoder.encode(value, Constants.UTF8);
        } catch (UnsupportedEncodingException e) {
            // Do nothing - UTF-8 should always be supported
        }
        return value;
    }

    @SuppressWarnings("rawtypes")
    public static void restoreSavedParametersIfAny(HttpServletRequest request, HttpServletResponse response, SsoRequest samlRequest) throws SamlException {
        IExtendedRequest extRequest = (IExtendedRequest) request;
        String postCookieName = COOKIE_NAME_SAVED_PARAMS + SamlUtil.hash(samlRequest.getProviderName());
        byte[] savePostIdBytes = extRequest.getCookieValueAsBytes(postCookieName); // **this may be urlEconded
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "savePostIdBytes:", savePostIdBytes);
        }
        if (savePostIdBytes == null || savePostIdBytes.length < 8) // length ought to be 12
            return; // no cookie found

        String savePostId = null;
        try {
            savePostId = new String(savePostIdBytes, Constants.UTF8);
        } catch (UnsupportedEncodingException e) {
            //
            throw new SamlException(e);
            // This should not happen since the encoding is utf-8
        }
        String cacheKey = SamlUtil.hash(savePostId);
        HttpRequestInfo requestInfo = (HttpRequestInfo) postCache.get(cacheKey);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "requestInfo is:", requestInfo);
        }
        if (requestInfo != null) {
            String callingUrl = request.getRequestURL().toString(); // keep this for restore the saved parameters (on SAMLReqponseTai)
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "callingUrl:", callingUrl);
                Tr.debug(tc, "reqUrl:", requestInfo.reqUrl);
            }
            if (callingUrl.equals(requestInfo.reqUrl)) {
                // Found and need to restore the savedPostParams
                extRequest.setMethod(METHOD_POST); // put the method as POST
                try {
                    extRequest.setInputStreamData((HashMap) requestInfo.savedPostParams);
                } catch (IOException e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "An exception setting InputStreamData : ", new Object[] { e });
                    }
                    throw new SamlException(e);
                }
                // clean up cookie and cached recordss
                RequestUtil.removeCookie(request,
                                         response,
                                         postCookieName);
                postCache.remove(cacheKey);
            }
        }
    }

    /**
     * @param req
     *
     */
    @SuppressWarnings("rawtypes")
    public void restorePostParams(HttpServletRequest request) {
        if (this.savedPostParams != null) {
            try {
                ((IExtendedRequest) request).setInputStreamData((HashMap) this.savedPostParams);
            } catch (IOException e) {

            }
        }
    }
}
