/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer40.srt.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.PushBuilder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet40.IRequest40;
import com.ibm.ws.session.SessionManagerConfig;
import com.ibm.ws.webcontainer40.osgi.osgi.WebContainerConstants;
import com.ibm.ws.webcontainer40.srt.SRTServletRequest40;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.ee8.Http2Request;

public class HttpPushBuilder implements PushBuilder, com.ibm.wsspi.http.ee8.Http2PushBuilder {

    private final static TraceComponent tc = Tr.register(HttpPushBuilder.class, WebContainerConstants.TR_GROUP, WebContainerConstants.NLS_PROPS);

    private String _method = "GET";
    private String _queryString = null;
    private String _sessionId = null;
    private String _path = null;
    private String _pathURI = null;
    private String _pathQueryString = null;
    private Enum _sessionIdFromCookieOrUrl;

    private static final String HDR_REFERER = HttpHeaderKeys.HDR_REFERER.getName();
    private static final String HDR_IF_MATCH = HttpHeaderKeys.HDR_IF_MATCH.getName();
    private static final String HDR_IF_MODIFIED_SINCE = HttpHeaderKeys.HDR_IF_MODIFIED_SINCE.getName();
    private static final String HDR_IF_NONE_MATCH = HttpHeaderKeys.HDR_IF_NONE_MATCH.getName();
    private static final String HDR_IF_RANGE = HttpHeaderKeys.HDR_IF_RANGE.getName();
    private static final String HDR_IF_UNMODIFIED_SINCE = HttpHeaderKeys.HDR_IF_UNMODIFIED_SINCE.getName();
    private static final String HDR_COOKIE = HttpHeaderKeys.HDR_COOKIE.getName();
    private static final String HDR_AUTHORIZATION = HttpHeaderKeys.HDR_AUTHORIZATION.getName();

    private final SRTServletRequest40 _inboundRequest;

    // Used to store headers for the push request
    HashMap<String, HashSet<HttpHeaderField>> _headers = new HashMap<String, HashSet<HttpHeaderField>>();

    private static ArrayList<String> _invalidMethods = new ArrayList<String>(Arrays.asList("POST",
                                                                                           "PUT",
                                                                                           "DELETE",
                                                                                           "CONNECT",
                                                                                           "OPTIONS",
                                                                                           "TRACE"));

    public HttpPushBuilder(SRTServletRequest40 request, String sessionId, Enumeration<String> headerNames, Cookie[] addedCookies) {

        _inboundRequest = request;
        _sessionId = sessionId;
        Set<SessionTrackingMode> sessionTrackingMode = request.getServletContext().getEffectiveSessionTrackingModes();

        if (request.isRequestedSessionIdFromCookie()) {
            _sessionIdFromCookieOrUrl = SessionTrackingMode.COOKIE; // set to COOKIE if session Id is coming from the Cookie
        } else if (request.isRequestedSessionIdFromURL()) {
            _sessionIdFromCookieOrUrl = SessionTrackingMode.URL; // set to URL if session Id is coming from the URL
        } else { // default
            if (sessionTrackingMode.contains(SessionTrackingMode.COOKIE)) {
                _sessionIdFromCookieOrUrl = SessionTrackingMode.COOKIE;
            } else {
                _sessionIdFromCookieOrUrl = SessionTrackingMode.URL;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "HttpPushBuilder", "_sessionIdFromCookieOrUrl = " + _sessionIdFromCookieOrUrl);
        }

        if (headerNames != null) {
            SessionManagerConfig smc = _inboundRequest.getWebAppDispatcherContext().getWebApp().getSessionContext().getWASSessionConfig();

            // Note: headers passed should already have removed the conditional headers.
            while (headerNames.hasMoreElements()) {

                String headerName = headerNames.nextElement();
                Enumeration<String> headerValues = request.getHeaders(headerName);
                while (headerValues.hasMoreElements()) {
                    String headerValue = headerValues.nextElement();

                    // If the header value contains the session cookie header, don't add it to the Push Builder headers
                    if (!headerValue.contains(smc.getSessionCookieName())) {
                        addHeader(headerName, headerValue);
                    }
                }
            }
        }

        // If the request is authenticated, an Authorization header should be set to the PushBuilder
        if (request.getUserPrincipal() != null) {
            // Set PushBuilder's Authorization header with the same value as the original request
            this.setHeader(HDR_AUTHORIZATION, request.getHeader(HDR_AUTHORIZATION));
        }

        if (addedCookies != null) {
            for (Cookie cookie : addedCookies) {
                if (cookie.getMaxAge() > 0) {
                    // Need to add the Cookie to the headers for the push request. Cookies were
                    // added to the response and therefore not included in the Cookie request header.
                    // Without adding this a call to getHeader("Cookie") would return null.
                    addHeader(HDR_COOKIE, cookie.getName() + "=" + cookie.getValue());
                }
            }
        }

        // set the REFERER header
        String referer = _inboundRequest.getRequestURL().toString();
        String queryString = _inboundRequest.getQueryString();

        if (queryString != null) {
            referer += "?" + queryString;
        }

        this.setHeader(HDR_REFERER, referer);

    }

    @Override
    public PushBuilder method(String method) throws IllegalArgumentException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "method()", "method = " + method);
        }

        if (method == null)
            throw new NullPointerException();
        else if (method.isEmpty())
            throw new IllegalArgumentException();
        else if (_invalidMethods.contains(method.toUpperCase())) {
            throw new IllegalArgumentException();
        }
        _method = method;
        return this;
    }

    @Override
    public PushBuilder queryString(String queryString) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "queryString()", "queryString = " + queryString);
        }
        _queryString = queryString;
        return this;
    }

    @Override
    public PushBuilder sessionId(String sessionId) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "sessionId()", "sessionId = " + sessionId);
        }
        _sessionId = sessionId;
        return this;
    }

    @Override
    public PushBuilder setHeader(String name, String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setHeader()", "name = " + name + ", value = " + value);
        }
        removeHeader(name);
        addHeader(name, value);
        return this;
    }

    @Override
    public PushBuilder addHeader(String name, String value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "addHeader()", "name = " + name + ", value = " + value);
        }
        if (_headers.containsKey(name)) {
            HashSet<HttpHeaderField> values = _headers.get(name);
            values.add(new HttpHeaderField(name, value));
        } else {
            HashSet<HttpHeaderField> values = new HashSet<HttpHeaderField>();
            values.add(new HttpHeaderField(name, value));
            _headers.put(name, values);
        }
        return this;
    }

    @Override
    public PushBuilder removeHeader(String name) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "removeHeader()", "name = " + name);
        }
        _headers.remove(name);
        return this;
    }

    @Override
    public PushBuilder path(String path) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "path()", "path = " + path);
        }

        if (path != null && !path.startsWith("/")) {
            String baseUri = _inboundRequest.getContextPath();
            if (baseUri != null) {
                if (!baseUri.endsWith("/")) {
                    baseUri = baseUri + "/";
                }
                path = baseUri + path;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "path()", "new context-relative path = " + path);
                }
            }
        }
        _path = path;

        if (path != null && path.contains("?")) {
            String[] pathParts = path.split("\\?");
            _pathURI = pathParts[0];
            _pathQueryString = "?" + pathParts[1];
        } else {
            _pathURI = path;
            _pathQueryString = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "path()", "uri = " + _pathURI + ", queryString = " + _pathQueryString);
        }
        return this;
    }

    @Override
    public void push() throws IllegalStateException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "push()", "path = " + _path);
        }

        if (_path == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "push()", "Path not set. Throw IllegalStateException");
            }
            throw new IllegalStateException();
        }

        if (_queryString != null) {
            if (_pathQueryString != null) {
                _pathQueryString += "&" + _queryString;
            } else {
                _pathQueryString = "?" + _queryString;
            }
        }

        if (_pathQueryString != null) {
            // if the session Id is coming from a URL, append it to the queryString
            if ((_sessionIdFromCookieOrUrl == SessionTrackingMode.URL) && _sessionId != null) {
                String urlRewritePrefix = _inboundRequest.getWebAppDispatcherContext().getWebApp().getSessionContext().getWASSessionConfig().getSessUrlRewritePrefix();
                _pathQueryString += urlRewritePrefix + _sessionId;
            }
        } else {
            // if the session Id is coming from a URL, append it to the URI
            if ((_sessionIdFromCookieOrUrl == SessionTrackingMode.URL) && _sessionId != null) {
                String urlRewritePrefix = _inboundRequest.getWebAppDispatcherContext().getWebApp().getSessionContext().getWASSessionConfig().getSessUrlRewritePrefix();
                _pathURI += urlRewritePrefix + _sessionId;
            }
        }

        // If the session is coming from a Cookie, then add that Cookie to the PushBuilder headers
        if ((_sessionIdFromCookieOrUrl == SessionTrackingMode.COOKIE) && _sessionId != null) {
            SessionManagerConfig smc = _inboundRequest.getWebAppDispatcherContext().getWebApp().getSessionContext().getWASSessionConfig();

            // Need to add the Cookie to the headers for the push request.
            addHeader(HDR_COOKIE, smc.getSessionCookieName() + "=" + _sessionId);
        }

        IRequest40 request = (IRequest40) _inboundRequest.getIRequest();
        ((Http2Request) request.getHttpRequest()).pushNewRequest(this);
        reset();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "push()");
        }

    }

    @Override
    public String getMethod() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getMethod()", "method = " + _method);
        }
        return _method;
    }

    @Override
    public String getQueryString() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getQueryString()", "queryString = " + _queryString);
        }
        return _queryString;
    }

    @Override
    public String getSessionId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getSessionId()", "sessionId = " + _sessionId);
        }
        return _sessionId;
    }

    @Override
    public Set<String> getHeaderNames() {
        return Collections.unmodifiableSet(_headers.keySet());
        //return _pushRequest.getAllHeaderNames();
    }

    @Override
    public String getHeader(String name) {
        HashSet<HttpHeaderField> values = _headers.get(name);
        if (values != null) {
            Iterator<HttpHeaderField> valuesIterator = values.iterator();
            // return the first value
            HttpHeaderField field = valuesIterator.next();
            return field.asString();
        } else
            return null;
    }

    @Override
    public String getPath() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getPath()", "path = " + _path);
        }
        return _path;
    }

    // Methods required by com.ibm.wsspi.http.ee8.HttpPushBuilder
    @Override
    public Set<HeaderField> getHeaders() {
        HashSet<HeaderField> headerFields = new HashSet<HeaderField>();

        if (_headers.size() > 0) {
            Iterator<String> headerNames = _headers.keySet().iterator();
            while (headerNames.hasNext()) {
                Iterator<HttpHeaderField> headers = _headers.get(headerNames.next()).iterator();
                while (headers.hasNext()) {
                    HttpHeaderField field = headers.next();
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getHeaders()", "add header name = " + field.getName() + ", value = " + field.asString());
                    }
                    headerFields.add(field);
                }
            }
        }

        return headerFields;
    }

    @Override
    public String getURI() {
        return this._pathURI;
    }

    @Override
    public String getPathQueryString() {
        return _pathQueryString;
    }

    // Reset the "state" of this PushBuilder before next push
    private void reset() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "reset()", "Clearing the path and removing conditional headers");
        }

        //clear the path
        _path = null;
        _pathURI = null;
        _queryString = null;
        _pathQueryString = null;

        //remove conditional headers
        removeHeader(HDR_IF_MATCH);
        removeHeader(HDR_IF_MODIFIED_SINCE);
        removeHeader(HDR_IF_NONE_MATCH);
        removeHeader(HDR_IF_RANGE);
        removeHeader(HDR_IF_UNMODIFIED_SINCE);
    }

}
