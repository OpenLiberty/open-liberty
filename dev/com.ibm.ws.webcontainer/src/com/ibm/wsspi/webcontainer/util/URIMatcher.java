/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.webcontainer.util;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer.servlet.ServletWrapper;
import com.ibm.ws.webcontainer.util.ClauseNode;
import com.ibm.ws.webcontainer.webapp.WebAppDispatcherContext;
import com.ibm.wsspi.webcontainer.WCCustomProperties;
import com.ibm.wsspi.webcontainer.logging.LoggerFactory;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

public class URIMatcher extends com.ibm.ws.webcontainer.util.URIMatcher {
    private static Logger logger = LoggerFactory.getInstance().getLogger("com.ibm.wsspi.webcontainer.util");
    private static final String CLASS_NAME = "com.ibm.wsspi.webcontainer.util.URIMatcher";
    protected static final boolean SERVLET_PATH_FOR_DEFAULT_MAPPING = Boolean.valueOf(WCCustomProperties.SERVLET_PATH_FOR_DEFAULT_MAPPING).booleanValue();

    public URIMatcher() {
        super();
    }

    public URIMatcher(boolean scalable) {
        super(scalable, true); //PM06111
    }

    /**
     * Method match.
     * 
     * Exactly the same logic as the above method, but this method will
     * populate the request with the servletPath, and pathInfo.
     * 
     * @param req
     * @return RequestProcessor
     */
    public Object match(IExtendedRequest req) {
        WebAppDispatcherContext dispatchContext = (WebAppDispatcherContext) req.getWebAppDispatcherContext();
        String uri = dispatchContext.getRelativeUri().trim();

        //		PK39337 - start
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
        if(WCCustomProperties.USE_SEMICOLON_AS_DELIMITER_IN_URI){
                String lowerCaseURI = uri.toLowerCase();
                if(jsessionIndex >= 0 && !lowerCaseURI.substring(jsessionIndex + 1).startsWith("jsessionid")){
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)){
                                logger.logp(Level.FINE, CLASS_NAME, "match", "The semi-colon is being treated as a delimiter");
                        }
                        //check if there is a jsessionid after the first semi-colon (example: /ctx_rt/servlet;1234;jsessionid=...)
                        //and check if it is valid (follows a semi-colon)
                        //if it is valid remove everything after the first semi-colon but keep the jsessionid
                        //if not remove everything after the semi-colon
                        int realJSIndex = lowerCaseURI.indexOf(";jsessionid");
                        if(realJSIndex < 0){
                                uri = uri.substring(0, jsessionIndex);
                                jsessionIndex = -1;
                        }
                        else{
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
            logger.logp(Level.FINE, CLASS_NAME, "match", "uri->" + uri);
            logger.logp(Level.FINE, CLASS_NAME, "match", "jsessionIndex->" + jsessionIndex);
        }
        
        //Special case for exact match on context root only
        if(uri.equals("/")){
            if(root.getTarget() != null){
                dispatchContext.setPossibleSlashStarMapping(false);
                //See 12.2 of Servlet specification for empty string URL pattern
                dispatchContext.setPathElements("", "/");
                if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, "match", "found an exact match on the context-root");
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
                logger.logp(Level.FINE, CLASS_NAME, "match", "found a target-->" + target);
            }

            boolean starTarget = node.getStarTarget() == target;
            String servletPath;
            String pathInfo = null;

            //Begin 313358, 61FVT:req.getPathInfo() returns incorrect values for spl chars
            if (starTarget) {
                // strip apart the uri
                int split = node.getDepth();
                servletPath = uri;
                // check if we haven't gone past the end (exact match with wildcard)
                if (split < uri.length()) {
                    servletPath = uri.substring(0, split);
                    pathInfo = uri.substring(split);
                }
            } else {
                // exact match
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
            logger.logp(Level.FINE, CLASS_NAME, "match", "looking for extension mapping");
        }

        target = findByExtension(strippedUri);
        if (target != null) {
            if (TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, "match", "uri-->" + strippedUri + ", target-->" + target);
            }
            //PK39337 - start
            dispatchContext.setPossibleSlashStarMapping(false);

            //PK39337 - end

            dispatchContext.setPathElements((jsessionIndex == -1) ? uri : uri.substring(0, jsessionIndex), (jsessionIndex == -1) ? null : uri.substring(jsessionIndex));
            return target;
        }

        // hit the defaultNode "/*"
        if (defaultNode != null) {
            //PK39337 - start
            dispatchContext.setPossibleSlashStarMapping(true);
            //PK39337 - end

            // PK80340 Start
            Object starTarget = defaultNode.getStarTarget();

            if (SERVLET_PATH_FOR_DEFAULT_MAPPING && (starTarget instanceof ServletWrapper) && ((ServletWrapper) starTarget).isDefaultServlet()) {
                dispatchContext.setPathElements(uri, null);
            } else {
                dispatchContext.setPathElements("", uri);
            }

            return starTarget;
            // PK80340 End
        }
        // not found
        return null;
    }
}
