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
import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.Extension;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsoc.Constants;
import com.ibm.ws.wsoc.ServiceManager;
import com.ibm.ws.wsoc.WebSocketContainerManager;
import com.ibm.ws.wsoc.injection.InjectionProvider;
import com.ibm.ws.wsoc.injection.InjectionProvider12;
import com.ibm.ws.wsoc.outbound.ClientConnector;

public class WebSocketContainerExt implements WebSocketContainer {

    private static final TraceComponent tc = Tr.register(WebSocketContainerExt.class);

    // config parameters that are owned at the Container level
    long defaultAsyncSendTimeout = 0; // don't timeout async sends unless told to do so by the user/app    
    long defaultMaxSessionIdleTimeout = -1; // default is no session timeout
    int defaultMaxBinaryMessageBufferSize = (int) Constants.DEFAULT_MAX_MSG_SIZE; // the max message size if not overridden by the annotated endpoint annotation 
    int defaultMaxTextMessageBufferSize = (int) Constants.DEFAULT_MAX_MSG_SIZE; // the max message size if not overridden by the annotated endpoint annotation

    @Override
    public long getDefaultAsyncSendTimeout() {
        return defaultAsyncSendTimeout;
    }

    @Override
    public void setAsyncSendTimeout(long timeoutmillis) {
        defaultAsyncSendTimeout = timeoutmillis;
    }

    @Override
    public long getDefaultMaxSessionIdleTimeout() {
        return defaultMaxSessionIdleTimeout;
    }

    @Override
    public void setDefaultMaxSessionIdleTimeout(long timeout) {
        defaultMaxSessionIdleTimeout = timeout;
    }

    @Override
    public int getDefaultMaxBinaryMessageBufferSize() {
        return defaultMaxBinaryMessageBufferSize;
    }

    @Override
    public void setDefaultMaxBinaryMessageBufferSize(int max) {
        defaultMaxBinaryMessageBufferSize = max;
    }

    @Override
    public int getDefaultMaxTextMessageBufferSize() {
        return defaultMaxTextMessageBufferSize;
    }

    @Override
    public void setDefaultMaxTextMessageBufferSize(int max) {
        defaultMaxTextMessageBufferSize = max;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.WebSocketContainer#connectToServer(java.lang.Object, java.net.URI)
     */
    @Override
    public Session connectToServer(Object clazz, URI path) throws DeploymentException, IOException {

        if (clazz == null || path == null) {
            throw new IllegalArgumentException();
        }
        ClientConnector x = new ClientConnector();
        return x.connectAnnotatedClass(clazz, path, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.WebSocketContainer#connectToServer(java.lang.Class, java.net.URI)
     */
    @Override
    public Session connectToServer(Class<?> clazz, URI path) throws DeploymentException, IOException {

        if (clazz == null || path == null) {
            throw new IllegalArgumentException();
        }
        Object theObject = null;
        ClientConnector x = new ClientConnector();

        try {
            theObject = getEndpointInstance(clazz);
        } catch (DeploymentException e) {
            String msg = Tr.formatMessage(tc, "client.connection.error", clazz.toString(), e.getMessage());
            Tr.error(tc, "client.connection.error", clazz.toString(), e.getMessage());
            throw new DeploymentException(msg, e);
        }

        return x.connectAnnotatedClass(theObject, path, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.WebSocketContainer#connectToServer(javax.websocket.Endpoint, javax.websocket.ClientEndpointConfig, java.net.URI)
     */
    @Override
    public Session connectToServer(Endpoint endpoint, ClientEndpointConfig endpointConfig, URI path) throws DeploymentException, IOException {

        if (endpoint == null || path == null) {
            throw new IllegalArgumentException();
        }
        ClientConnector x = new ClientConnector();
        return x.connectClass(endpoint, path, endpointConfig, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.WebSocketContainer#connectToServer(java.lang.Class, javax.websocket.ClientEndpointConfig, java.net.URI)
     */
    @Override
    public Session connectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig endpointConfig, URI path) throws DeploymentException, IOException {

        Object theObject = null;

        if (endpointClass == null || path == null) {
            throw new IllegalArgumentException();
        }

        if (!(Endpoint.class.isAssignableFrom(endpointClass))) {
            DeploymentException e = new DeploymentException("Class " + endpointClass.getName() + " does not extend Endpoint");
            String msg = Tr.formatMessage(tc, "client.invalid.endpointclass", endpointClass.toString(), e.getMessage());
            Tr.error(tc, "client.invalid.endpointclass", endpointClass.toString(), e.getMessage());
            throw new DeploymentException(msg, e);
        }

        ClientConnector x = new ClientConnector();

        try {
            theObject = getEndpointInstance(endpointClass);
        } catch (DeploymentException e) {
            String msg = Tr.formatMessage(tc, "client.connection.error", endpointClass.toString(), e.getMessage());
            Tr.error(tc, "client.connection.error", endpointClass.toString(), e.getMessage());
            throw new DeploymentException(msg, e);
        }

        return x.connectClass(theObject, path, endpointConfig, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.websocket.WebSocketContainer#getInstalledExtensions()
     */
    @Override
    public Set<Extension> getInstalledExtensions() {

        return Collections.emptySet();
    }

    public <T> T getEndpointInstance(Class<T> endpointClass) throws DeploymentException {
        try {

            InjectionProvider12 ip12 = ServiceManager.getInjectionProvider12();
            if (ip12 != null) {
                ConcurrentHashMap map = WebSocketContainerManager.getRef().getEndpointMap();
                T ep = ip12.getManagedEndpointInstance(endpointClass, map);
                if (ep != null) {
                    return ep;
                }
            } else {

                InjectionProvider ip = ServiceManager.getInjectionProvider();
                if (ip != null) {
                    ConcurrentHashMap map = WebSocketContainerManager.getRef().getEndpointMap();
                    T ep = ip.getManagedEndpointInstance(endpointClass, map);
                    if (ep != null) {
                        return ep;
                    }
                }
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "did not create the endpoint using the CDI service.  Will create the instance without CDI.");
            }

            return endpointClass.newInstance();
        } catch (IllegalAccessException e) {
            throw new DeploymentException(e.getLocalizedMessage());
        } catch (InstantiationException e) {
            throw new DeploymentException(e.getLocalizedMessage());
        }
    }

}
