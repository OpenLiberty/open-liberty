/*******************************************************************************
 * Copyright (c) 2011, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer40.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.MappingMatch;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.servlet.ServletWrapper;
import com.ibm.ws.webcontainer.util.ClauseNode;
import com.ibm.ws.webcontainer40.osgi.webapp.WebAppDispatcherContext40;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;
import com.ibm.wsspi.webcontainer.util.URIMatcher;

/**
 * A URIMatcher implementation that supports the Servlet 4.0 ServletMapping API.
 */
public class URIMatcher40 extends URIMatcher {

    protected static final Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.wsspi.webcontainer40.util");
    private static final String CLASS_NAME = URIMatcher40.class.getName();

    public URIMatcher40() {
        super();
    }

    public URIMatcher40(boolean scalable) {
        super(scalable);
    }

    /**
     * This method is the same as the parent implementation with the exception that it sets the
     * MappingMatch value for each match on the SRTServletRequestThreadData40.
     *
     * @param req
     * @return RequestProcessor
     */
    @Override
    public Object match(IExtendedRequest req) {
        String methodName = "match";

        WebAppDispatcherContext40 dispatchContext = (WebAppDispatcherContext40) req.getWebAppDispatcherContext();
        String uri = dispatchContext.getRelativeUri().trim();

        //              PK39337 - start
        // set default to true
        dispatchContext.setPossibleSlashStarMapping(true);
        //PK39337 - end

        /*
         * DO NOT DELETE THESE COMMENTS. They should be introduced in v7.
         * Fixes the case where we have two servlets A and B.
         * A is mapped to /servletA/*
         * B is mapped to /servletA/servletB
         * path info for /servletA/servletB/pathinfo returns /pathinfo, should be /servletB/pathinfo
         */

        int jsessionIndex = uri.indexOf(';');

        //start PI31292
        //Check if the semi-colon really belongs to jsessionid,
        //if not, reset the jsessionid index value to -1 and ignore
        //everything after the semi-colon
        if (WCCustomProperties.USE_SEMICOLON_AS_DELIMITER_IN_URI) {
            String lowerCaseURI = uri.toLowerCase();
            if (jsessionIndex >= 0 && !lowerCaseURI.substring(jsessionIndex + 1).startsWith("jsessionid")) {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "The semi-colon is being treated as a delimiter");
                }
                //check if there is a jsessionid after the first semi-colon (example: /ctx_rt/servlet;1234;jsessionid=...)
                //and check if it is valid (follows a semi-colon)
                //if it is valid remove everything after the first semi-colon but keep the jsessionid
                //if not remove everything after the semi-colon
                int realJSIndex = lowerCaseURI.indexOf(";jsessionid");
                if (realJSIndex < 0) {
                    uri = uri.substring(0, jsessionIndex);
                    jsessionIndex = -1;
                } else {
                    uri = uri.substring(0, jsessionIndex) + uri.substring(realJSIndex);
                }
            }
        }
        //end PI31292

        String strippedUri = uri;

        if (jsessionIndex != -1) {
            strippedUri = strippedUri.substring(0, jsessionIndex);
        }

        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "uri->" + uri);
            logger.logp(Level.FINE, CLASS_NAME, methodName, "jsessionIndex->" + jsessionIndex);
        }

        //Special case for exact match on context root only
        if (uri.equals("/")) {
            if (root.getTarget() != null) {
                dispatchContext.setPossibleSlashStarMapping(false);
                //See 12.2 of Servlet specification for empty string URL pattern
                // Servlet 4.0: context-root match
                dispatchContext.setMappingMatch(MappingMatch.CONTEXT_ROOT);
                dispatchContext.setPathElements("", "/");
                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "found an exact match on the context-root");
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "set mappingMatch to: " + MappingMatch.CONTEXT_ROOT);

                }
                return root.getTarget();
            }
        }

        Result result = findNode(strippedUri);

        Object target = null;
        // if we hit the default node (/*) we need to check extensions first
        if (result != null && result.node != defaultNode) {
            ClauseNode node = result.node;

            // found an exact or star match
            target = result.target;
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "found a target-->" + target);
            }

            boolean starTarget = node.getStarTarget() == target;
            String servletPath;
            String pathInfo = null;

            //Begin 313358, 61FVT:req.getPathInfo() returns incorrect values for spl chars
            if (starTarget) {
                // Servlet 4.0: path match
                dispatchContext.setMappingMatch(MappingMatch.PATH);
                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "set MappingMatch to: " + MappingMatch.PATH);
                }
                // strip apart the uri
                int split = node.getDepth();
                servletPath = uri;
                // check if we haven't gone past the end (exact match with wildcard)
                if (split < uri.length()) {
                    servletPath = uri.substring(0, split);
                    pathInfo = uri.substring(split);
                }
            } else {
                // Servlet 4.0: exact match
                dispatchContext.setMappingMatch(MappingMatch.EXACT);
                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "set MappingMatch to: " + MappingMatch.EXACT);
                }
                servletPath = strippedUri;
                pathInfo = (jsessionIndex == -1) ? null : uri.substring(jsessionIndex);
            }

            //PK39337 - start
            if (node != root) {
                dispatchContext.setPossibleSlashStarMapping(false);
            }
            //PK39337 - end

            dispatchContext.setPathElements(servletPath, pathInfo);
            //End 313358, 61FVT:req.getPathInfo() returns incorrect values for spl chars
            return target;
        }

        // extension matching
        //
        if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
            logger.logp(Level.FINE, CLASS_NAME, methodName, "looking for extension mapping");
        }

        target = findByExtension(strippedUri);
        if (target != null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "uri-->" + strippedUri + ", target-->" + target);
            }
            //PK39337 - start
            dispatchContext.setPossibleSlashStarMapping(false);

            //PK39337 - end

            dispatchContext.setPathElements((jsessionIndex == -1) ? uri : uri.substring(0, jsessionIndex), (jsessionIndex == -1) ? null : uri.substring(jsessionIndex));

            // Servlet 4.0: extension match
            dispatchContext.setMappingMatch(MappingMatch.EXTENSION);
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "set MappingMatch to: " + MappingMatch.EXTENSION);
            }
            return target;
        }

        // hit the defaultNode "/*"
        if (defaultNode != null) {

            // Servlet 4.0: default match
            dispatchContext.setMappingMatch(MappingMatch.DEFAULT);
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "set MappingMatch to: " + MappingMatch.DEFAULT);
            }

            //PK39337 - start
            dispatchContext.setPossibleSlashStarMapping(true);
            //PK39337 - end

            // PK80340 Start
            Object starTarget = defaultNode.getStarTarget();

            if (URIMatcher.SERVLET_PATH_FOR_DEFAULT_MAPPING && (starTarget instanceof ServletWrapper) && ((ServletWrapper) starTarget).isDefaultServlet()) {
                dispatchContext.setPathElements(uri, null);
            } else {
                dispatchContext.setPathElements("", uri);
            }

            return starTarget;
            // PK80340 End
        }
        return null;
    }
}
