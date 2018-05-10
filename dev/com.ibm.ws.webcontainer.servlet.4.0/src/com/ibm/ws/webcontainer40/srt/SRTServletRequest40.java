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
package com.ibm.ws.webcontainer40.srt;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.MappingMatch;
import javax.servlet.http.PushBuilder;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.servlet.request.IRequest;
import com.ibm.websphere.servlet40.IRequest40;
import com.ibm.ws.webcontainer31.srt.SRTServletRequest31;
import com.ibm.ws.webcontainer40.osgi.srt.SRTConnectionContext40;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.ws.webcontainer40.srt.http.HttpPushBuilder;
import com.ibm.ws.webcontainer40.srt.http.HttpServletMappingImpl;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.ee8.Http2Request;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IServletWrapper;
import com.ibm.wsspi.webcontainer40.WCCustomProperties40;

public class SRTServletRequest40 extends SRTServletRequest31 implements HttpServletRequest {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.ws.webcontainer40.srt");
    private static final String CLASS_NAME = "com.ibm.ws.webcontainer40.srt.SRTServletRequest40";

    private boolean _sessionCreated;
    private Enumeration<String> _pushBuilderHeaders;

    private static ArrayList<String> _disallowedPushBuilderHeaders;

    HashMap<String, String> _trailers;

    static {
        _disallowedPushBuilderHeaders = new ArrayList<String>();

        // Conditional headers
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_IF_MATCH.getName());
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_IF_MODIFIED_SINCE.getName());
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_IF_NONE_MATCH.getName());
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_IF_RANGE.getName());
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_IF_UNMODIFIED_SINCE.getName());

        // Range Headers
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_RANGE.getName());

        // Expect Headers
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_EXPECT.getName());

        // Authorization headers
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_AUTHORIZATION.getName());

        // Referrer Headers
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_REFERER.getName());

        // HTTP2 headers
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_UPGRADE.getName());
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_HTTP2_SETTINGS.getName());
        _disallowedPushBuilderHeaders.add(HttpHeaderKeys.HDR_CONNECTION.getName());

    }

    public SRTServletRequest40(SRTConnectionContext40 context) {
        super(context);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initForNextRequest(IRequest req) {
        String methodName = "initForNextRequest";

        // 321485
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.logp(Level.FINE, CLASS_NAME, methodName, "this->" + this + " : " + " req->" + req);
        }
        _sessionCreated = false;
        super.initForNextRequest(req);
        _pushBuilderHeaders = null;
        _trailers = null;
    }

    @Override
    public HttpServletMapping getHttpServletMapping() {
        String methodName = "getHttpServletMapping";

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, methodName);
        }

        WebAppDispatcherContext40 dispatchContext = (WebAppDispatcherContext40) this.getDispatchContext();

        HttpServletMapping returnMapping = dispatchContext.getServletMapping();
        if (returnMapping != null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "existing mapping found. Servlet name = " + returnMapping.getServletName());
            }
            return returnMapping;

        }

        return this.getCurrentHttpServletMapping(dispatchContext);
    }

    public HttpServletMapping getCurrentHttpServletMapping(WebAppDispatcherContext40 dispatchContext) {
        String methodName = "getCurrentHttpServletMapping";

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, methodName + " dispatchContext -> " + dispatchContext);
        }

        HttpServletMapping returnMapping = null;

        if (dispatchContext.getMappingMatch() != null) {

            // Get the servlet name
            IServletWrapper servletRef = dispatchContext.getCurrentServletReference();
            String servletName = null;
            if (servletRef != null)
                servletName = servletRef.getServletName();

            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "servletName was set to: " + servletName);
            }

            // Get the pathInfo and servletPath
            String pathInfo = dispatchContext.getPathInfoForMapping();
            String servletPath = dispatchContext.getServletPathForMapping();

            if (pathInfo == null) {
                pathInfo = "";
            }

            if (servletPath == null) {
                servletPath = "";
            }

            // Calculate the initial matchValue
            String matchValue = servletPath + pathInfo;

            // matchValue should not start with "/"
            if (matchValue.startsWith("/")) {
                matchValue = matchValue.substring(1, matchValue.length());
            }

            // Initial pattern
            String pattern = "/";

            switch (dispatchContext.getMappingMatch()) {
                case CONTEXT_ROOT:
                    // matchValue and pattern are both the empty string
                    returnMapping = new HttpServletMappingImpl(MappingMatch.CONTEXT_ROOT, "", "", servletName);
                    break;
                case DEFAULT:
                    // matchValue is the empty string and the pattern is "/"
                    returnMapping = new HttpServletMappingImpl(MappingMatch.DEFAULT, "", pattern, servletName);
                    break;
                case EXACT:
                    // matchValue and pattern are the same in this case except matchValue has no leading "/"
                    pattern = servletPath + pathInfo;
                    returnMapping = new HttpServletMappingImpl(MappingMatch.EXACT, matchValue, pattern, servletName);
                    break;
                case EXTENSION:
                    // matchValue is everything before the extension (".") and the pattern is "/*" + the extension including (".") taken from the servletPath.
                    matchValue = matchValue.substring(0, matchValue.indexOf("."));
                    pattern = "*" + servletPath.substring(servletPath.indexOf("."), servletPath.length());
                    returnMapping = new HttpServletMappingImpl(MappingMatch.EXTENSION, matchValue, pattern, servletName);
                    break;
                case PATH:
                    // matchValue is the pathInfo after the last "/" and pattern is the servletPath + "/*"
                    matchValue = pathInfo.substring(pathInfo.lastIndexOf("/") + 1, pathInfo.length());
                    pattern = servletPath + "/*";
                    returnMapping = new HttpServletMappingImpl(MappingMatch.PATH, matchValue, pattern, servletName);
                    break;
                default:
                    // If nothing else matches we should return UNKNOWN
                    returnMapping = new HttpServletMappingImpl(null, "", "", "");
                    break;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "matching match not found.");
            }
            returnMapping = new HttpServletMappingImpl(null, "", "", "");
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, methodName);
        }

        return returnMapping;
    }

    @Override
    public PushBuilder newPushBuilder() {
        String methodName = "newPushBuilder";

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.entering(CLASS_NAME, methodName, "this -> " + this);
        }

        IRequest40 iRequest = (IRequest40) getIRequest();
        if (!((Http2Request) iRequest.getHttpRequest()).isPushSupported()) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
                logger.logp(Level.FINE, CLASS_NAME, methodName, "push not supported");
            }
            return null;
        }

        String sessionID = null;
        if (_sessionCreated)
            sessionID = getSession(false).getId();
        else
            sessionID = getRequestedSessionId();

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.logp(Level.FINE, CLASS_NAME, methodName, "sessionId = " + sessionID);
        }

        SRTServletResponse40 response = (SRTServletResponse40) this._connContext.getResponse();

        PushBuilder pb = new HttpPushBuilder(this, sessionID, getPushBuilderHeaders(), response.getAddedCookies());

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.exiting(CLASS_NAME, methodName);
        }
        return pb;

    }

    /**
     * Returns the session as an HttpSession. This does all of the "magic"
     * to create the session if it doesn't already exist.
     */
    @Override
    public HttpSession getSession(boolean create) {
        String methodName = "getSession";

        // 321485
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) { //306998.15
            logger.logp(Level.FINE, CLASS_NAME, methodName, "create " + String.valueOf(create) + ", this -> " + this);
        }

        HttpSession session = super.getSession(create);

        if (session != null) {
            _sessionCreated = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "_sessionCreated " + _sessionCreated + ", this -> " + this);
        }

        return session;
    }

    private Enumeration<String> getPushBuilderHeaders() {

        if (_pushBuilderHeaders == null) {
            ArrayList<String> pushBuilderHeaderNames = new ArrayList<String>();
            Enumeration<String> allHeaders = this.getHeaderNames();
            while (allHeaders.hasMoreElements()) {
                String headerName = allHeaders.nextElement();
                if (!_disallowedPushBuilderHeaders.contains(headerName)) {
                    pushBuilderHeaderNames.add(headerName);
                }
            }
            _pushBuilderHeaders = Collections.enumeration(pushBuilderHeaderNames);
        }

        return _pushBuilderHeaders;

    }

    @Override
    public String getCharacterEncoding() {
        String methodName = "getCharacterEncoding";

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, methodName, "this -> " + this);
        }

        String _encoding = super.getSrtHelperCharEncoding();

        if (_encoding != null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "returns -> " + _encoding);
            }
            return _encoding;
        }

        String type = getContentType();

        int index = -1;
        if (type != null)
            index = type.indexOf("charset=");
        _encoding = getEncodingFromContentType(type, index);

        if (_encoding != null) {
            try {
                setCharacterEncoding(_encoding);
            } catch (UnsupportedEncodingException e) {
                logger.logp(Level.INFO, CLASS_NAME, methodName, "Unable to set request character encoding based upon request header ", e);
            }
        }

        if (_encoding == null) {
            _encoding = getDispatchContext().getWebApp().getConfiguration().getModuleRequestEncoding();

            if (_encoding != null) {
                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "Encoding from web module ->[" + _encoding + "]");

                try {
                    setCharacterEncoding(_encoding);
                } catch (UnsupportedEncodingException e) {
                    logger.logp(Level.INFO, CLASS_NAME, methodName, "Unable to set request character encoding", e);
                }
            }
        }

        if (_encoding == null) {
            _encoding = WCCustomProperties40.SERVER_REQUEST_ENCODING;

            if (_encoding != null && !_encoding.isEmpty()) {
                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE))
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "Encoding from WC property->[" + _encoding + "]");

                try {
                    setCharacterEncoding(_encoding);
                } catch (UnsupportedEncodingException e) {
                    logger.logp(Level.INFO, CLASS_NAME, methodName, "Unable to set request character encoding", e);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, methodName, "encoding -> " + _encoding);
        }

        return _encoding;
    }

    @Override
    public Map<String, String> getTrailerFields() throws IllegalStateException {
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.entering(CLASS_NAME, "getTrailerFields", "this -> " + this);
        }

        if (!isTrailerFieldsReady())
            throw new IllegalStateException();

        if (_trailers == null) {

            IRequest40 request = (IRequest40) getIRequest();

            _trailers = request.getTrailers();

        }
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.exiting(CLASS_NAME, "getTrailerFields", "number of trailers -> " + _trailers.size());
        }

        return _trailers;
    }

    @Override
    public boolean isTrailerFieldsReady() {
        IRequest40 request = (IRequest40) getIRequest();

        return request.getHttpRequest().isTrailersReady();
    }

}
