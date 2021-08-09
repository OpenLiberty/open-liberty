/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.webcontainer31.osgi.webapp.WebApp31;
import com.ibm.ws.wsoc.external.ServerContainerExt;
import com.ibm.ws.wsoc.external.WsocHandlerImpl;

/**
 * 
 */
@HandlesTypes({ ServerEndpoint.class, Endpoint.class, ServerApplicationConfig.class })
public class WebSocketServletContainerInitializer implements ServletContainerInitializer {

    private static final TraceComponent tc = Tr.register(WebSocketServletContainerInitializer.class);

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContainerInitializer#onStartup(java.util.Set, javax.servlet.ServletContext)
     */
    @Override
    public void onStartup(Set<Class<?>> clazzes, ServletContext servletContext) throws ServletException {

        try {
            WsocHandlerImpl wsocServletHandler = new WsocHandlerImpl();
            ((WebApp31) servletContext).registerWebSocketHandler(wsocServletHandler);

            ServerContainerExt serverContainer = new ServerContainerExt();

            servletContext.setAttribute(WebSocketContainerManager.SERVER_CONTAINER_ATTRIBUTE, serverContainer);

            WsocServletContextListener wscl = new WsocServletContextListener();
            wscl.initialize(serverContainer.getEndpointManager());
            servletContext.addListener(wscl);

            WsocHttpSessionListener listener = new WsocHttpSessionListener();
            listener.initialize(serverContainer.getEndpointManager());
            servletContext.addListener(listener);

            if (clazzes != null) {
                String endPointContext = servletContext.getContextPath();
                Set<EndpointHelper> annotatedEndpoints = getAnnotatedServerEndpointClasses(clazzes, endPointContext, serverContainer);
                Set<EndpointHelper> programmaticEndpoints = getServerEndpointClasses(clazzes, endPointContext);
                Set<Class<? extends ServerApplicationConfig>> serverAppConfigs = getServerApplicationConfigClasses(clazzes);

                Set<EndpointHelper> helpers = determineEndpoints(annotatedEndpoints, programmaticEndpoints, serverAppConfigs);

                for (EndpointHelper helper : helpers) {
                    serverContainer.addEndpoint(helper);
                }
            }
            //  findbugs wanted runtime exception first...
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception f) {
            // allow instrumented FFDC to be used here

            //DeploymentException needs to stop the application from starting
            if (f instanceof DeploymentException) {
                throw new RuntimeException(f);
            }
        }
    }

    private Set<EndpointHelper> getAnnotatedServerEndpointClasses(Set<Class<?>> classes, String contextPath, ServerContainerExt serverContainer) {
        if (classes == null) {
            return Collections.emptySet();
        }

        Set<EndpointHelper> endpointAnnotationHelpers = new HashSet<EndpointHelper>();

        for (Class<?> c : classes) {
            EndpointHelper eh = serverContainer.getAnnotatedEndpointClass(c);
            if (eh != null) {
                endpointAnnotationHelpers.add(eh);
            }
        }

        return endpointAnnotationHelpers;
    }

    private Set<EndpointHelper> getServerEndpointClasses(Set<Class<?>> classes, String contextPath) {

        Set<EndpointHelper> serverEndpoints = new HashSet<EndpointHelper>();

        for (Class<?> c : classes) {
            if ((Endpoint.class).isAssignableFrom(c)) {
                EndpointHelper helper = new EndpointHelper();
                helper.setEndpointClass(c);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "possible programmatic endpoint of: " + c);
                }
                serverEndpoints.add(helper);
            }
        }

        return serverEndpoints;
    }

    private Set<Class<? extends ServerApplicationConfig>> getServerApplicationConfigClasses(Set<Class<?>> classes) {

        Set<Class<? extends ServerApplicationConfig>> serverApplicationConfigs = new HashSet<Class<? extends ServerApplicationConfig>>();

        for (Class<?> c : classes) {
            if ((ServerApplicationConfig.class).isAssignableFrom(c)) {

                @SuppressWarnings("unchecked")
                Class<? extends ServerApplicationConfig> x = (Class<? extends ServerApplicationConfig>) c;

                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "found ServerApplicationConfig of: " + x);
                }
                serverApplicationConfigs.add(x);
            }
        }

        return serverApplicationConfigs;
    }

    // See section 6.2 of the WebSocket spec for how this algorithm is suppose to work
    public Set<EndpointHelper> determineEndpoints(Set<EndpointHelper> annotatedEndpointHelpers,
                                                  Set<EndpointHelper> programmaticEndpointHelpers,
                                                  Set<Class<? extends ServerApplicationConfig>> serverAppConfigs) {

        Set<Class<?>> annotatedEndpointClasses = new HashSet<Class<?>>();
        HashMap<Class<?>, EndpointHelper> endpointMap = new HashMap<Class<?>, EndpointHelper>();
        Set<EndpointHelper> returnHelpers = new HashSet<EndpointHelper>();

        Set<Class<? extends Endpoint>> programmaticEndpointClasses = new HashSet<Class<? extends Endpoint>>();
        Set<Class<? extends ServerEndpointConfig>> endpointConfigs = new HashSet<Class<? extends ServerEndpointConfig>>();

        // no ServerApplicationConfig objects, allow all the annotated endpoints through, but no programmatic endpoints
        if (serverAppConfigs.size() == 0) {
            return annotatedEndpointHelpers;
        }

        // first look at annotated Endpoints
        // build the set of annotated Endpoints to look through, also need a map of endpoints to helpers for later indexing
        for (EndpointHelper helper : annotatedEndpointHelpers) {
            Class<?> c = helper.getEndpointClass();
            annotatedEndpointClasses.add(c);
            // need a map of EndpointClass to EndpointHelper to index into later
            endpointMap.put(c, helper);
        }

        // if we have annotated endpoints, then pass the set of them to each user config object
        if (annotatedEndpointClasses.size() > 0) {
            for (Class<? extends ServerApplicationConfig> configClass : serverAppConfigs) {
                ServerApplicationConfig config = null;
                try {
                    config = configClass.newInstance();
                } catch (IllegalAccessException e) {
                    // allow instrumented FFDC to be used here
                } catch (InstantiationException e) {
                    // allow instrumented FFDC to be used here
                }

                if (config != null) {
                    // get from the user config object the configs for which endpoints to use
                    Set<Class<?>> annotatedEnpointsToUse = config.getAnnotatedEndpointClasses(annotatedEndpointClasses);

                    // for each returned endpoint class, find the endpoint helper and put it into the return set
                    for (Class<?> c : annotatedEnpointsToUse) {
                        EndpointHelper helper = endpointMap.get(c);
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "will use annoated endpoint of: " + c);
                        }
                        returnHelpers.add(helper);
                    }
                }
            }
        }

        // now do the same with programmatic configs.
        // first look at programmatic Endpoints
        // build the set of programmatic Endpoints to look through, also need a map of endpoints to helpers for later indexing
        endpointMap.clear();
        for (EndpointHelper helper : programmaticEndpointHelpers) {
            Class<?> c = helper.getEndpointClass();
            programmaticEndpointClasses.add((Class<? extends Endpoint>) c);
            // need a map of EndpointClass to EndpointHelper to index into later
            endpointMap.put(c, helper);
        }

        // if we have programmatic endpoints, then pass the set of them to each user config object
        if (programmaticEndpointClasses.size() > 0) {
            for (Class<? extends ServerApplicationConfig> configClass : serverAppConfigs) {
                ServerApplicationConfig config = null;
                try {
                    config = configClass.newInstance();
                } catch (IllegalAccessException e) {
                    // allow instrumented FFDC to be used here
                } catch (InstantiationException e) {
                    // allow instrumented FFDC to be used here
                }

                if (config != null) {
                    // get from the user config object which of the programmatic endpoints to use
                    Set<ServerEndpointConfig> programmaticEnpointConfigsToUse = config.getEndpointConfigs(programmaticEndpointClasses);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "programmaticEnpointConfigsToUse: " + programmaticEnpointConfigsToUse);
                    }
                    if (programmaticEnpointConfigsToUse != null) {
                        // for each returned endpoint config class, create the endpoint helper and put it into the return set
                        for (ServerEndpointConfig endpointConfig : programmaticEnpointConfigsToUse) {

                            Class<?> endpoint = endpointConfig.getEndpointClass();
                            // Question: is this really how we should map the endpoint from the user config, to the endpoint we scanned earlier?
                            // You can map a single endpoint to multiple URIs/configurators, etc, so creating new here.
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "endpointConfig class: " + endpoint.getName());
                            }
                            EndpointHelper scannedHelper = endpointMap.get(endpoint);
                            if (scannedHelper != null) {
                                EndpointHelper newHelper = new EndpointHelper();
                                newHelper.setEndpointClass(scannedHelper.getEndpointClass());

                                newHelper.setServerEndpointConfig(endpointConfig);
                                if (tc.isDebugEnabled()) {
                                    Tr.debug(tc, "will use programmatic endpoint of: " + endpoint);
                                }
                                returnHelpers.add(newHelper);
                            }
                        }
                    }
                }
            }
        }

        return returnHelpers;
    }
}
