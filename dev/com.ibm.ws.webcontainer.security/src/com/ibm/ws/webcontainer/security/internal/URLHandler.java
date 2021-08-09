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
package com.ibm.ws.webcontainer.security.internal;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.util.WSUtil;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;

/**
 * Generic URL handler. Provides various support for URL operations.
 * Unless a method is explicitly needed by a class outside of this package,
 * methods should be left as default scope.
 * 
 */
public class URLHandler {
    private static final TraceComponent tc = Tr.register(URLHandler.class);
    protected final WebAppSecurityConfig webAppSecConfig;

    public URLHandler(WebAppSecurityConfig webAppSecConfig) {
        this.webAppSecConfig = webAppSecConfig;
    }

    /**
     * Encode the specified URL String with percent encoding.
     * 
     * Order of encoding:
     * 1. Encode percent signs to "%25". Not doing this first will clobber subsequent encodings.
     * 2. Encode semi-colons to "%3B" since they are invalid in cookie values.
     * 3. Encode commas to "%2C".
     * 
     * @param url a non-null String
     * @return encoded version of url
     */
    @Sensitive
    protected String encodeURL(@Sensitive String url) {
        url = url.replaceAll("%", "%25");
        url = url.replaceAll(";", "%3B");
        url = url.replaceAll(",", "%2C");

        return url;
    }

    /**
     * Decode the specified URL String from percent encoding.
     * 
     * Order of decoding:
     * 1. Decode "%2C" to commas.
     * 2. Decode "%3B" to semi-colons.
     * 3. Decode "%25" to percent signs.
     * 
     * Technically decoding order is not that important, but its best to undo
     * the encoding in the same order.
     * 
     * @param url a non-null String
     * @return decoded version of url
     */
    @Sensitive
    protected String decodeURL(@Sensitive String url) {

        url = url.replaceAll("%2C", ",");
        url = url.replaceAll("%3B", ";");
        url = url.replaceAll("%25", "%");

        return url;
    }

    /**
     * Remove only the host name from the specified URL String.
     * 
     * @param url A valid URL string
     * @return
     */
    @Sensitive
    protected String removeHostNameFromURL(@Sensitive String url) {
        try {
            int doubleSlash = url.indexOf("//");
            int firstSingleSlash = url.indexOf("/", doubleSlash + 2);
            URL originalURL = new URL(url);
            StringBuffer workURL = new StringBuffer();
            workURL.append(originalURL.getProtocol());
            workURL.append("://");
            int port = originalURL.getPort();
            if (port != -1) {
                workURL.append(":");
                workURL.append(port);
            }
            url = workURL.append(url.substring(firstSingleSlash)).toString();
        } catch (java.net.MalformedURLException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "removeHostNameFromURL", new Object[] { e });
            }
        }
        return url;
    }

    /**
     * Updates the referrerURL with the host for the current request
     * if one is missing. If referrerURL is relative or an empty String,
     * use the host for the current request and append the referrerURL.
     * Otherwise, inject into the referrer URL String the host for the
     * current request.
     * 
     * Note, this method does not handle the following scenarios:
     * - either storeReq or URLString is null (could they ever be?)
     * - URLString being incomplete, e.g. http://myhost.com (missing first /)
     * 
     * @param referrerURL A valid URL string, potentially without host name, from the referrer URL cookie
     * @param url A valid, fully qualified URL representing the current request
     * @return
     */
    @Sensitive
    protected String restoreHostNameToURL(@Sensitive String referrerURL, String url) {
        if ((referrerURL.startsWith("/")) || (referrerURL.length() == 0)) {
            int doubleSlash = url.indexOf("//");
            int firstSingleSlash = url.indexOf("/", doubleSlash + 2);
            referrerURL = url.substring(0, firstSingleSlash) + referrerURL;
        } else {
            try {
                URL referrer = new URL(referrerURL);
                String referrerHost = referrer.getHost();
                if ((referrerHost == null) || (referrerHost.length() == 0)) {
                    URL currentURL = new URL(url);
                    String currentHost = currentURL.getHost();
                    int doubleSlash = referrerURL.indexOf("//");
                    StringBuffer newURLBuf = new StringBuffer(referrerURL);
                    newURLBuf.insert(doubleSlash + 2, currentHost);
                    referrerURL = newURLBuf.toString();
                }
            } catch (java.net.MalformedURLException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "restoreHostNameToURL", new Object[] { e });
                }
            }
        }
        return referrerURL;
    }

    /**
     * Return the full servlet path, including any additional path info.
     * 
     * getRequestURI includes the ContextPath of the URL. As per Servlet 2.2
     * specifications, URL mapping does not include the ContextPath. Thus,
     * we should use ServletPath + PathInfo.
     * 
     * @param req HttpServletRequest
     */
    public String getServletURI(HttpServletRequest req) {
        String uriName = req.getServletPath();
        String pathInfo = req.getPathInfo();

        if (pathInfo != null)
            uriName = uriName.concat(pathInfo);
        if (uriName == null || uriName.length() == 0)
            uriName = "/";

        uriName = WSUtil.resolveURI(uriName);
        int sindex;
        if ((sindex = uriName.indexOf(";")) != -1) {
            uriName = uriName.substring(0, sindex);
        }

        //we need to ensure we are following the java EE spec and handle a colon in the uri
        if (uriName.indexOf(":") >= 0) {
            uriName = uriName.replaceAll(":", "%3A");
        }

        return uriName;
    }

}