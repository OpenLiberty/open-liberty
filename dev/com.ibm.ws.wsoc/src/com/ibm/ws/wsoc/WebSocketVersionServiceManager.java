/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsoc;

import java.io.InputStream;
import java.util.Properties;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsoc.external.WebSocketFactory;
import com.ibm.ws.wsoc.outbound.HttpRequestor;
import com.ibm.ws.wsoc.outbound.HttpRequestorFactory;
import com.ibm.ws.wsoc.outbound.HttpRequestorWsoc10FactoryImpl;
import com.ibm.ws.wsoc.servercontainer.ServletContainerFactory;
import com.ibm.ws.wsoc.servercontainer.v10.ServerContainerImplFactory10;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Provides various services for differnet features.
 */
public class WebSocketVersionServiceManager {

    private static final TraceComponent tc = Tr.register(WebSocketVersionServiceManager.class);

    /** CHFWBundle service reference -- required */
    private static final AtomicServiceReference<CHFWBundle> cfwBundleRef = new AtomicServiceReference<CHFWBundle>("chfwBundle");

    //websocket 1.1 SessionExt for WebSocket 1.1 API support
    private static final AtomicServiceReference<WebSocketFactory> websocketFactoryServiceRef = new AtomicServiceReference<WebSocketFactory>("websocketFactoryService");

    private static final AtomicServiceReference<ServletContainerFactory> servletContainerFactorySRRef = new AtomicServiceReference<ServletContainerFactory>("servletContainerFactoryService");

    private static final WebSocketFactory DEFAULT_WEBSOCKET_FACTORY = new WebSocketFactoryImpl();

    private static final ServletContainerFactory DEFAULT_SERVLET_CONTAINER_FACTORY = new ServerContainerImplFactory10();

    private static final AtomicServiceReference<HttpRequestorFactory> httpRequestorFactoryServiceRef =
                    new AtomicServiceReference<HttpRequestorFactory>("httpRequestorFactoryService");

    private static final AtomicServiceReference<ClientEndpointConfigCopyFactory> clientEndpointConfigCopyFactoryServiceRef =
                    new AtomicServiceReference<ClientEndpointConfigCopyFactory>("clientEndpointConfigCopyFactoryService");

    private static final HttpRequestorFactory DEFAULT_HTTPREQUESTOR_FACTORY = new HttpRequestorWsoc10FactoryImpl();

    public static String LOADED_SPEC_LEVEL = loadWsocVersion();

    private static String DEFAULT_VERSION = "1.0";

    /**
     * DS method for activating this component.
     *
     * @param context
     */
    protected synchronized void activate(ComponentContext context) {
        cfwBundleRef.activate(context);
        websocketFactoryServiceRef.activate(context);
        servletContainerFactorySRRef.activate(context);
        httpRequestorFactoryServiceRef.activate(context);
        clientEndpointConfigCopyFactoryServiceRef.activate(context);
    }

    /**
     * DS method for deactivating this component.
     *
     * @param context
     */
    protected synchronized void deactivate(ComponentContext context) {
        cfwBundleRef.deactivate(context);
        websocketFactoryServiceRef.deactivate(context);
        servletContainerFactorySRRef.deactivate(context);
        httpRequestorFactoryServiceRef.deactivate(context);
        clientEndpointConfigCopyFactoryServiceRef.deactivate(context);
    }

    /**
     * DS method for setting the event reference.
     *
     * @param service
     */
    protected void setChfwBundle(ServiceReference<CHFWBundle> service) {
        cfwBundleRef.setReference(service);
    }

    /**
     * DS method for removing the event reference.
     *
     * @param service
     */
    protected void unsetChfwBundle(ServiceReference<CHFWBundle> service) {
        cfwBundleRef.unsetReference(service);
    }

    /**
     * @return ChannelFramework associated with the CHFWBundle service.
     */
    public static ChannelFramework getCfw() {
        return cfwBundleRef.getServiceWithException().getFramework();
    }

    /**
     * Access the current reference to the bytebuffer pool manager from channel frame work.
     *
     * @return WsByteBufferPoolManager
     */
    public static WsByteBufferPoolManager getBufferPoolManager() {
        if (cfwBundleRef.getService() != null) {
            return cfwBundleRef.getService().getBufferManager();
        }
        return ChannelFrameworkFactory.getBufferManager();
    }

    public static WebSocketFactory getWebSocketFactory() {
        //if websocket 1.1 feature is enabled, then get WebSocketFactoryV11 instance, else use the default
        //WebSocketFactoryV10 instance
        WebSocketFactory webSocketFactory = websocketFactoryServiceRef.getService();
        if (webSocketFactory == null) {
            return DEFAULT_WEBSOCKET_FACTORY;
        }

        return webSocketFactory;
    }

    protected static ServletContainerFactory getServerContainerExtFactory() {
        // if websocket 2.1 is enabled, use ServerContainerImplFactory21, else use default (ServerContainerImplFactory10)
        ServletContainerFactory servletContainerFactory = servletContainerFactorySRRef.getService();
        if (servletContainerFactory != null) {
            return servletContainerFactory;
        }
        return DEFAULT_SERVLET_CONTAINER_FACTORY;
    }

    protected void setServletContainerFactoryService(ServiceReference<ServletContainerFactory> service) {
        servletContainerFactorySRRef.setReference(service);
    }

    protected void unsetServletContainerFactoryService(ServiceReference<ServletContainerFactory> service) {
        servletContainerFactorySRRef.unsetReference(service);
    }

    protected void setWebsocketFactoryService(ServiceReference<WebSocketFactory> ref) {
        websocketFactoryServiceRef.setReference(ref);
    }

    protected void unsetWebsocketFactoryService(ServiceReference<WebSocketFactory> ref) {
        websocketFactoryServiceRef.unsetReference(ref);
    }

    public static HttpRequestorFactory getHttpRequestorFactory() {
        // if websocket 2.1 is enabled, use HttpRequestorWsoc21FactoryImpl, else use default (HttpRequestorWsoc10FactoryImpl)
        HttpRequestorFactory httpRequestorFactory = httpRequestorFactoryServiceRef.getService();
        if (httpRequestorFactory == null) {
            return DEFAULT_HTTPREQUESTOR_FACTORY;
        }
        return httpRequestorFactory;
    }

    protected void setHttpRequestorFactoryService(ServiceReference<HttpRequestorFactory> ref) {
        httpRequestorFactoryServiceRef.setReference(ref);
    }

    protected void unsetHttpRequestorFactoryService(ServiceReference<HttpRequestorFactory> ref) {
        httpRequestorFactoryServiceRef.unsetReference(ref);
    }

    public static ClientEndpointConfigCopyFactory getClientEndpointConfigCopyFactory() {
        // Only used if websocket 2.1 is enabled
        ClientEndpointConfigCopyFactory clientEndpointConfigCopyFactory = clientEndpointConfigCopyFactoryServiceRef.getService();
        return clientEndpointConfigCopyFactory;
    }

    protected void setClientEndpointConfigCopyFactoryService(ServiceReference<ClientEndpointConfigCopyFactory> ref) {
        clientEndpointConfigCopyFactoryServiceRef.setReference(ref);
    }

    protected void unsetClientEndpointConfigCopyFactoryService(ServiceReference<ClientEndpointConfigCopyFactory> ref) {
        clientEndpointConfigCopyFactoryServiceRef.unsetReference(ref);
    }

    private static synchronized String loadWsocVersion(){

        try (InputStream input = WebSocketVersionServiceManager.class.getClassLoader().getResourceAsStream("io/openliberty/wsoc/speclevel/wsocSpecLevel.properties")) {

            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                String version = prop.getProperty("version");
                Tr.debug(tc, "Loading WebSocket version " + version + " from wsocSpecLevel.propertie");
                return version;
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "InputStream was null for wsocSpecLevel.properties");
                }
            }

        } catch (Exception ex) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Exception occured: " + ex.getCause());
            }
        }

        Tr.error(tc, "wsoc.feature.not.loaded.correctly");

        return WebSocketVersionServiceManager.DEFAULT_VERSION;
    }

    public static boolean isWsoc21OrHigher() {
        if (Double.parseDouble(WebSocketVersionServiceManager.LOADED_SPEC_LEVEL) >= 2.1) {
            return true;
        }
        return false;
    }

}
