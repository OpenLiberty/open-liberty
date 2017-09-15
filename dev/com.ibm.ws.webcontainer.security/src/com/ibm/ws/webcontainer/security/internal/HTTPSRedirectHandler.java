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

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.security.WebRequest;

/**
 *
 */
public class HTTPSRedirectHandler {
    private static final TraceComponent tc = Tr.register(HTTPSRedirectHandler.class);

    /**
     * Determines if HTTPS redirect is required for this request.
     * 
     * @param webRequest
     * @return {@code true} if the request requires SSL but the request is not secure, {@code false} otherwise.
     */
    public boolean shouldRedirectToHttps(WebRequest webRequest) {
        HttpServletRequest req = webRequest.getHttpServletRequest();
        return !req.isSecure() && webRequest.isSSLRequired();
    }

    /**
     * Get the new URL for the redirect which contains the https port.
     * 
     * @param req
     * @return WebReply to the redirect URL, or a 403 is any unexpected behaviour occurs
     */
    public WebReply getHTTPSRedirectWebReply(HttpServletRequest req) {

        Integer httpsPort = (Integer) SRTServletRequestUtils.getPrivateAttribute(req, "SecurityRedirectPort");
        if (httpsPort == null) {
            Tr.error(tc, "SSL_PORT_IS_NULL");
            // return a 403 if we don't know what the port is
            return new DenyReply("Resource must be accessed with a secure connection try again using an HTTPS connection.");
        }

        URL originalURL = null;
        String urlString = null;
        try {
            urlString = req.getRequestURL().toString();
            originalURL = new URL(urlString);
        } catch (MalformedURLException e) {
            Tr.error(tc, "SSL_REQ_URL_MALFORMED_EXCEPTION", urlString);
            // return a 403 if we can't construct the redirect URL
            return new DenyReply("Resource must be accessed with a secure connection try again using an HTTPS connection.");
        }
        String queryString = req.getQueryString();
        try {
            URL redirectURL = new URL("https",
                            originalURL.getHost(),
                            httpsPort,
                            originalURL.getPath() + (queryString == null ? "" : "?" + queryString));

            //don't add cookies during the redirect as this results in duplicated and incomplete
            //cookies on the client side
            return new RedirectReply(redirectURL.toString(), null);
        } catch (MalformedURLException e) {
            Tr.error(tc, "SSL_REQ_URL_MALFORMED_EXCEPTION", "https" + originalURL.getHost() + httpsPort + originalURL.getPath() + (queryString == null ? "" : "?" + queryString));
            // return a 403 if we can't construct the redirect URL
            return new DenyReply("Resource must be accessed with a secure connection try again using an HTTPS connection.");
        }
    }
}
