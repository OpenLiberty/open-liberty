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

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.ws.wsoc.external.WebSocketFactory;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.channelfw.ChannelFramework;
import com.ibm.wsspi.channelfw.ChannelFrameworkFactory;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 *
 */
public class WebSocketVersionServiceManager {

    /** CHFWBundle service reference -- required */
    private static final AtomicServiceReference<CHFWBundle> cfwBundleRef = new AtomicServiceReference<CHFWBundle>("chfwBundle");

    //websocket 1.1 SessionExt for WebSocket 1.1 API support
    private static final AtomicServiceReference<WebSocketFactory> websocketFactoryServiceRef =
                    new AtomicServiceReference<WebSocketFactory>("websocketFactoryService");

    private static final WebSocketFactory DEFAULT_WEBSOCKET_FACTORY = new WebSocketFactoryImpl();

    /**
     * DS method for activating this component.
     * 
     * @param context
     */
    protected synchronized void activate(ComponentContext context) {
        cfwBundleRef.activate(context);
        websocketFactoryServiceRef.activate(context);
    }

    /**
     * DS method for deactivating this component.
     * 
     * @param context
     */
    protected synchronized void deactivate(ComponentContext context) {
        cfwBundleRef.deactivate(context);
        websocketFactoryServiceRef.deactivate(context);
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

    protected void setWebsocketFactoryService(ServiceReference<WebSocketFactory> ref) {
        websocketFactoryServiceRef.setReference(ref);
    }

    protected void unsetWebsocketFactoryService(ServiceReference<WebSocketFactory> ref) {
        websocketFactoryServiceRef.unsetReference(ref);
    }
}
