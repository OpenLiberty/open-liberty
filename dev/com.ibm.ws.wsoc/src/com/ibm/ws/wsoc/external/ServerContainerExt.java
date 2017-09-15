/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.Decoder;
import javax.websocket.DeploymentException;
import javax.websocket.Encoder;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import javax.websocket.server.ServerEndpointConfig.Builder;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.wsoc.WsWsocServerContainer;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.wsoc.AnnotatedEndpoint;
import com.ibm.ws.wsoc.EndpointHelper;
import com.ibm.ws.wsoc.EndpointManager;

public class ServerContainerExt extends WebSocketContainerExt implements WsWsocServerContainer {

    private static final TraceComponent tc = Tr.register(ServerContainerExt.class);

    EndpointManager endpointManager = new EndpointManager();

    private boolean noMoreAdds = false;

    private final WsocHandlerImpl wsocUpgradeHandler = new WsocHandlerImpl();

    public void initialize() {}

    @Override
    public void addEndpoint(Class<?> endpointClass) throws DeploymentException {

        if (noMoreAdds) {
            String msg = Tr.formatMessage(tc, "endpoint.addsclosed");
            throw new IllegalStateException(msg);
        }

        EndpointHelper helper = getAnnotatedEndpointClass(endpointClass);
        //check if this endpoint already exists. This can happen in 2 scenarios
        //1) when a servlet code is adding the endpoint class explicitly calling this method and the 
        //same endpoint class is already discovered during webapp start up through WebSocketServletContainerInitializer.onStartUp() path.
        //2) when class scanning is turned off during web app startup and no discovery of server endpoint has been done during startup.
        //and servelet code  is adding the endpoint classes explicitly calling this method.
        String path = helper.getInvocationPath();
        if (endpointManager.isURIExists(path)) { //check if URI already exists for this webapp
            ServerEndpointConfig serverConfig = endpointManager.getServerEndpointConfig(path);
            if (serverConfig.getEndpointClass().equals(endpointClass)) { //further check if endpoint class is also the same
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Endpoint class:" + endpointClass + " with URI: " + path + " has already been processed during webapp startup.");
                }
                return;
            } else { //user/servlet code is calling this method for 2 different classes with same URI which is a error case
                cleanupEndpoints();
                String msg = Tr.formatMessage(tc,
                                              "duplicate.uri",
                                              serverConfig.getPath());
                Tr.error(tc,
                         "duplicate.uri",
                         serverConfig.getPath());
                throw new IllegalStateException(msg);
            }
        } //no need of else {} check here because if URI is different then endpoint class will be different in that webapp 

        try {
            addEndpoint(findOrBuildServerConfig(helper));
        } catch (IllegalAccessException e) {
            String msg = Tr.formatMessage(tc, "endpoint.creation.error", endpointClass, e);
            throw new DeploymentException(msg);

        } catch (InstantiationException e) {
            String msg = Tr.formatMessage(tc, "endpoint.creation.error", endpointClass, e);
            throw new DeploymentException(msg);
        }
    }

    @Override
    public void addEndpoint(ServerEndpointConfig serverConfig) throws DeploymentException {

        if (noMoreAdds) {
            String msg = Tr.formatMessage(tc, "endpoint.addsclosed");
            throw new IllegalStateException(msg);
        }

        //Section 3.1.1 URI Mapping - "An application that contains multiple endpoint paths that are the same relative URI 
        //is not a valid application. An application that contains multiple endpoint paths that are equivalent URI-templates 
        //is not a valid application. [WSC-3.1.1-2]"
        if (endpointManager.isURIExists(serverConfig.getPath())) {
            cleanupEndpoints();
            String msg = Tr.formatMessage(tc,
                                          "duplicate.uri",
                                          serverConfig.getPath());
            Tr.error(tc,
                     "duplicate.uri",
                     serverConfig.getPath());
            throw new DeploymentException(msg);
        }

        try {
            createAnnotatedEndpoint(serverConfig);
        } catch (DeploymentException e) {
            //cleanup endpoints which were deployed prior to the exception 
            cleanupEndpoints();
            throw e;
        }

        endpointManager.addServerEndpointConfig(serverConfig);

    }

    public void addEndpoint(EndpointHelper helper) throws DeploymentException, IllegalAccessException, InstantiationException {
        addEndpoint(findOrBuildServerConfig(helper));
    }

    private void cleanupEndpoints() {
        endpointManager.clear();
    }

    private String addWildCard(String path) {
        String delimiter = "/";
        String[] tockenizedPath = path.split(delimiter);
        StringBuffer buffer = new StringBuffer();
        for (String part : tockenizedPath) {
            if (!part.isEmpty()) {
                if (part.startsWith("{") && part.endsWith("}")) {
                    buffer.append(delimiter + "*");
                    //Currently, loop breaks as soon as it finds the first segment to replace the wildcard 
                    //For e.g if the path is /bookings/{guest-id}/hotel/{name},  this logic results in  /bookings/* not /bookings/*/hotel/*
                    //The reason is webcontainer does seem to take the mapping with multiple wild cards in it for e.g /bookings/*/hotel/*
                    //hence we  register as /bookings/* which works fine for both cases, /bookings/{guest-id}/hotel/{name} or /bookings/{guest-id},
                    //and runtime will get the current ServerConfig based on uri mapping logic.                     
                    break;
                } else {
                    buffer.append(delimiter + part);
                }
            }
        }
        return buffer.toString();
    }

    public ServerEndpointConfig getServerEndpointConfig(String _path) {
        ServerEndpointConfig config = endpointManager.getServerEndpointConfig(_path);
        return config;
    }

    public EndpointManager getEndpointManager() {
        return endpointManager;
    }

    public void setNoMoreAddEndpointsAllowed() {
        noMoreAdds = true;

    }

    @FFDCIgnore({ IllegalStateException.class })
    public ServerEndpointConfig findOrBuildServerConfig(EndpointHelper helper) throws IllegalAccessException, InstantiationException, DeploymentException {

        if (helper.getServerEndpointConfig() == null) {
            String mapping = helper.getInvocationPath();
            Builder builder = null;
            try {
                // create builder with endpoint class
                builder = ServerEndpointConfig.Builder.create(helper.getEndpointClass(), mapping);
            } catch (IllegalStateException e) { //can occur if the server endpoint uri is either null or uri path doesn't start with forward slash '/' 
                String msg = Tr.formatMessage(tc,
                                              "missingslashornull.uri",
                                              helper.getEndpointClass(), helper.getInvocationPath());
                Tr.error(tc,
                         "missingslashornull.uri",
                         helper.getEndpointClass(), helper.getInvocationPath());
                throw new DeploymentException(msg); //this should stop from app from deploying/starting 
            }
            // add decoders and encoder classes for this endpoint, if they exist
            if (helper.getDecoders() != null) {
                builder.decoders(helper.getDecoders());
            }
            if (helper.getEncoders() != null) {
                builder.encoders(helper.getEncoders());
            }

            if (helper.getServerEndpointConfigurator() != null) {
                builder.configurator(helper.getServerEndpointConfigurator().newInstance());
            }

            if (helper.getSubprotocols() != null) {
                builder.subprotocols(Arrays.asList(helper.getSubprotocols()));
            }

            return builder.build();
        }
        else {
            return helper.getServerEndpointConfig();
        }

    }

    public EndpointHelper getAnnotatedEndpointClass(Class<?> c) {
        ServerEndpoint se = c.getAnnotation(ServerEndpoint.class);
        EndpointHelper helper = null;

        if (se != null) {
            helper = new EndpointHelper();
            helper.setEndpointClass(c);
            helper.setInvocationPath(se.value());

            Class<? extends Decoder>[] decoders = se.decoders();
            if ((decoders != null) && (decoders.length > 0))
            {
                ArrayList<Class<? extends Decoder>> list = new ArrayList<Class<? extends Decoder>>();
                for (int i = 0; i < decoders.length; i++) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "found decoder of: " + decoders[i]);
                    }
                    list.add(decoders[i]);
                }

                helper.setDecoders(list);
            }

            Class<? extends Encoder>[] encoders = se.encoders();
            if ((encoders != null) && (encoders.length > 0))
            {
                ArrayList<Class<? extends Encoder>> list = new ArrayList<Class<? extends Encoder>>();
                for (int i = 0; i < encoders.length; i++) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "found encoder of: " + encoders[i]);
                    }
                    list.add(encoders[i]);
                }

                helper.setEncoders(list);
            }

            Class<? extends ServerEndpointConfig.Configurator> configurator = se.configurator();
            if ((configurator != null))
            {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "found configurator of: " + configurator);
                }

                helper.setServerEndpointConfigurator(configurator);
            }
            String[] subprotocols = se.subprotocols();
            if (subprotocols != null) {
                if (tc.isDebugEnabled()) {
                    for (String protocol : subprotocols) {
                        Tr.debug(tc, "found a subprotocol " + protocol);
                    }
                }

                helper.setSubprotocols(subprotocols);
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "possible annotated endpoint of: " + c);
            }

        }
        return helper;
    }

    private AnnotatedEndpoint createAnnotatedEndpoint(ServerEndpointConfig serverConfig) throws DeploymentException {
        AnnotatedEndpoint annotatedEP = null;
        if (serverConfig.getEndpointClass().isAnnotationPresent(ServerEndpoint.class)) {
            annotatedEP = new AnnotatedEndpoint();
            annotatedEP.initialize(serverConfig.getEndpointClass(), serverConfig);
            endpointManager.addAnnotatedEndpoint(annotatedEP);
        }
        return annotatedEP;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.websphere.wsoc.WsWsocServerContainer#upgrade(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, javax.websocket.EndpointConfig,
     * java.lang.String)
     */
    @Override
    public void doUpgrade(HttpServletRequest request, HttpServletResponse response, ServerEndpointConfig endpointConfig, Map<String, String> pathParams) throws ServletException, IOException {

        wsocUpgradeHandler.handleRequest(request, response, endpointConfig, pathParams, true);
        if (!response.isCommitted()) {
            response.getOutputStream().close();
        }

    }

}
