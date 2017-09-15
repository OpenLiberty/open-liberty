/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc.external;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;
import com.ibm.ws.webcontainer.servlet.WsocHandler;
import com.ibm.ws.wsoc.AnnotatedEndpoint;
import com.ibm.ws.wsoc.HandshakeProcessor;
import com.ibm.ws.wsoc.ParametersOfInterest;
import com.ibm.ws.wsoc.WebSocketContainerManager;
import com.ibm.ws.wsoc.WsocUpgradeHandler;
import com.ibm.ws.wsoc.util.Utils;

/**
 *
 */
public class WsocHandlerImpl implements WsocHandler {

    private static final TraceComponent tc = Tr.register(WsocHandlerImpl.class);
    boolean noMoreAddsCalled = false;

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.servlet.WsocServletHandler#invoke(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    @Override
    public boolean isWsocRequest(ServletRequest request) throws ServletException {
        ServletContext context = request.getServletContext();
        Object wsocContainer = null;
        if (context != null) {
            wsocContainer = context.getAttribute(WebSocketContainerManager.SERVER_CONTAINER_ATTRIBUTE);
        }

        if (wsocContainer != null) {
            //check if this is a IBM's implementation of WebSocketContainer. If this is a tomcat or some other server's WebSocket
            //implementation of WebSocketContainer then, this is not a valid WebSocket request for us and Web Container should not call
            //WsocHandler.handleRequest() subsequently.
            if (!isValidContainer(wsocContainer)) {
                return false;
            }
        } else { //not a WebSocket request if ServerContainer attribute is missing.
            return false;
        }

        //call setNoMoreAddEndPoints only once per webapp
        if (!noMoreAddsCalled) {
            ((ServerContainerExt) wsocContainer).setNoMoreAddEndpointsAllowed();
            noMoreAddsCalled = true;
        }

        return checkWebSocketRequest(request, wsocContainer);
    }

    private boolean isValidContainer(Object wsocContainer) {
        if (wsocContainer instanceof com.ibm.ws.wsoc.external.ServerContainerExt) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkWebSocketRequest(ServletRequest req, Object wsocContainer) throws ServletException {

        if (checkIfUpgradeHeader(req)) {
            // determine the websocket container for this web module, and the websocket endpoint class for this request
            ServerContainerExt container = (ServerContainerExt) wsocContainer;
            String path = ((HttpServletRequest) req).getServletPath();

            String pathInfo = ((HttpServletRequest) req).getPathInfo();
            if (pathInfo != null) {
                path = path + pathInfo;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "servlet path is: " + path);
            }

            ServerEndpointConfig endPointConfig = container.getServerEndpointConfig(path);
            if (endPointConfig != null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "WebSocket request");
                }
                return true;
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Not a websocket request");
        }
        return false;
    }

    private boolean checkIfUpgradeHeader(ServletRequest request) {
        boolean upgrade = false;
        boolean websocket = false;
        String headerValue = null;

        if (request instanceof HttpServletRequest) {
            if ("GET".equals(((HttpServletRequest) request).getMethod())) {
                Enumeration<String> names = ((HttpServletRequest) request).getHeaderNames();
                while (names.hasMoreElements()) {
                    String name = names.nextElement();
                    //check if it's websocket upgrade connection.
                    if (name.equalsIgnoreCase("upgrade")) {
                        upgrade = true;
                        headerValue = ((HttpServletRequest) request).getHeader(name);
                    }
                    if (headerValue != null && headerValue.equalsIgnoreCase("websocket")) {
                        websocket = true;
                    }
                    if (upgrade && websocket) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "WebSocket upgrade header exists");
                        }
                        return true;
                    }
                }
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WebSocket upgrade header doesn't exist");
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.webcontainer.servlet.WsocServletHandler#handleRequest(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) {
        handleRequest(request, response, null, null, false);
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response, ServerEndpointConfig endPointConfig, Map<String, String> paramMap,
                              boolean allowEndpointCreation) {
        WsocUpgradeHandler wuh = null;
        Class<?> endPointClass = null;

        //   Read and verify headers before we move on to anything else.
        HandshakeProcessor hp = new HandshakeProcessor();
        hp.initialize(request, response, paramMap);
        try {
            hp.readRequestInfo();
            hp.verifyHeaders();
        } catch (Exception e) {
            // allow instrumented FFDC to be used here
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "header validation failed, throwing following exception. Exception message: " + e.getMessage());
            }
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;

        }

        // determine the websocket container for this web module, and the websocket endpoint class for this request
        ServletContext context = request.getServletContext();
        ServerContainerExt container = (ServerContainerExt) context.getAttribute(WebSocketContainerManager.SERVER_CONTAINER_ATTRIBUTE);
        String path = request.getServletPath();

        //If servlet is registered with mapping which contains wildcard (*), then look for pathInfo as well. For e.g servlet is mapped with
        //the path /bookings/*  and if websocket endpoint is  invoked with ws://localhost:9080/WebSocketTestWar/bookings/JohnDoe
        //pathInfo /JohnDoe. If there is no wildcard in servlet mapping, pathInfo is null.
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            path = path + pathInfo;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "servlet path is: " + path);
        }
        if (endPointConfig == null) {
            endPointConfig = container.getServerEndpointConfig(path);
        }

        if (endPointConfig != null) {
            endPointClass = endPointConfig.getEndpointClass();
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "returning: server endpoint config not found");
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        ServerEndpointConfig.Configurator configurator = endPointConfig.getConfigurator();
        hp.addWsocConfigurationData(endPointConfig, configurator);

        if (!hp.checkOrigin()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Configurator checkOrigin return false, sending 403.");
            }
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Endpoint ep = null;

        try {
            Object instance = configurator.getEndpointInstance(endPointClass);

            if (endPointClass.isAnnotationPresent(ServerEndpoint.class)) {
                try {
                    AnnotatedEndpoint aep = container.getEndpointManager().getAnnotatedEndpoint(endPointConfig.getEndpointClass());
                    if (aep == null && allowEndpointCreation) {

                        aep = new AnnotatedEndpoint();
                        aep.initialize(endPointClass, endPointConfig);
                        // We currently do not need to add this to ServerEndpointConfig map in endpoint manager as that map is only used pre-deploy.
                        container.getEndpointManager().addAnnotatedEndpoint(aep);
                    }
                    if (aep != null) {
                        aep = (AnnotatedEndpoint) aep.clone();
                        aep.setAppInstance(instance);
                        aep.setRequestPath(path);
                        ep = aep;
                    } else {
                        Tr.error(tc, "endpoint.instance.error", endPointClass);
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        return;
                    }
                } catch (Exception e) {
                    throw new ServletException(e);
                }
            } else {
                ep = (Endpoint) instance;
            }
        } catch (InstantiationException e) {
            Tr.error(tc, "endpoint.instance.error", endPointClass, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        } catch (Throwable t) {
            Tr.error(tc, "endpoint.instance.error", endPointClass, t);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        try {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "requesting our upgrade handler be instantiated");
            }
            wuh = request.upgrade(WsocUpgradeHandler.class);
        } catch (IOException e) {
            // allow instrumented FFDC to be used here
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "returning: user configurator threw an InstantiationException. Exception message: " + e.getMessage());
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;

        } catch (ServletException e) {
            // allow instrumented FFDC to be used here
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "returning: user configurator threw an InstantiationException. Exception message: " + e.getMessage());
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (wuh == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "returning: could not get our upgrade handler instantiated");
            }
            return;
        }

        wuh.initialize(ep, endPointConfig, container);

        hp.addResponseHeaders();
        hp.determineAndSetSubProtocol(); // Question: do we need to re-determine this based on the headers after the user has modified the response?
        hp.determineAndSetExtensions();
        hp.modifyHandshake();

        ParametersOfInterest poi = hp.getParametersOfInterest();
        poi.setEndpointManager(container.getEndpointManager());

        ClassLoader tccl = Utils.getContextClassloaderPrivileged();
        ComponentMetaData componentMetaData = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "WsocServlet: stored CL of: " + tccl);
            Tr.debug(tc, "WsocServlet: stored CMD of: " + componentMetaData);
        }

        poi.setTccl(tccl);
        poi.setCmd(componentMetaData);

        response.setStatus(101);

        wuh.setParametersOfInterest(poi);

    }
}
