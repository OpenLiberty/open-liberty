/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.webcontainer.srt.SRTServletRequest;

/**
 *
 */
public class ServletRequestInfoDebug {

    private static final TraceComponent tc = Tr.register(ServletRequestInfoDebug.class);
    static final String AUTHORIZATION_HEADER = "Authorization";
    static final String MASKED_BASIC_AUTH = "Basic xxxxx";
    static final String DEBUG_HEADER_EYECATCHER = "Http Header names and values:";
    static final int DEBUG_REQ_INFO_BUFSIZE = 512;
    static final String DEBUG_CONTEXT_PATH = "Request Context Path=";
    static final String DEBUG_SERVLET_PATH = ", Servlet Path=";
    static final String DEBUG_PATH_INFO = ", Path Info=";
    static final String DEBUG_REMOTE_ADDRESS = ", Remote Address=";
    static final String DEBUG_REMOTE_PORT = ", Remote Port=";

    @Trivial
    static void logServerRequestInfo(HttpServletRequest req) {
        if (tc.isDebugEnabled() && req != null) {
            Tr.debug(tc, servletRequestInfo(req));
        }
    }

    /**
     * Collect all the Http header names and values.
     *
     * @param req HttpServletRequest
     * @return Returns a string that contains each parameter and it value(s)
     *         in the HttpServletRequest object.
     */
    @Trivial
    static String servletRequestInfo(HttpServletRequest req) {

        StringBuffer sb = new StringBuffer(DEBUG_REQ_INFO_BUFSIZE);
        sb.append(DEBUG_HEADER_EYECATCHER).append("\n");

        try {
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                sb.append(headerName).append("=");
                sb.append("[").append(getHeader(req, headerName)).append("]\n");
            }
        } catch (Throwable t) {
            // do nothing because it probably means the parser was trying to parse
            // non form data or serialized object data.  This is a trace issue and
            // has nothing to do with the spec.
        }

        if (req.getContextPath() != null)
            sb.append(DEBUG_CONTEXT_PATH).append(req.getContextPath());
        if (req.getServletPath() != null)
            sb.append(DEBUG_SERVLET_PATH).append(req.getServletPath());
        if (req.getPathInfo() != null)
            sb.append(DEBUG_PATH_INFO).append(req.getPathInfo());
        String remoteAddress = req.getHeader("X-FORWARDED-FOR");
        if (remoteAddress == null) {
            remoteAddress = req.getRemoteAddr();
        }
        sb.append(DEBUG_REMOTE_ADDRESS).append(remoteAddress);
        sb.append(DEBUG_REMOTE_PORT).append(req.getRemotePort());

        return sb.toString();
    }

    /**
     *
     * This method returns header information of the incoming HttpServletRequest
     *
     * @param req HttpServletRequest
     * @param key String (header name)
     * @return Returns a string value for the given header in the HttpServletRequest object.
     *
     **/
    @Trivial
    static String getHeader(HttpServletRequest req, String key) {
        HttpServletRequest sr = req;
        String header = null;

        if (sr instanceof HttpServletRequestWrapper) {
            HttpServletRequestWrapper w = (HttpServletRequestWrapper) sr;
            // make sure we drill all the way down to an SRTServletRequest...there
            // may be multiple proxied objects
            sr = (HttpServletRequest) w.getRequest();
            while (sr != null && sr instanceof HttpServletRequestWrapper)
                sr = (HttpServletRequest) ((HttpServletRequestWrapper) sr).getRequest();
        }

        if (sr != null && sr instanceof SRTServletRequest) {
            // Cast and return result
            header = ((SRTServletRequest) sr).getHeaderDirect(key);
        } else {
            header = req.getHeader(key);
        }

        if (key != null && key.equalsIgnoreCase(AUTHORIZATION_HEADER) && header != null && header.startsWith("Basic")) {
            header = MASKED_BASIC_AUTH;
        }

        return ((header == null) ? "" : header);
    }

}
