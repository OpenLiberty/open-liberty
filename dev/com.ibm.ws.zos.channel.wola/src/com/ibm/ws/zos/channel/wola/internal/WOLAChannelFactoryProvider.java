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
package com.ibm.ws.zos.channel.wola.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.event.EventEngine;
import com.ibm.ws.zos.channel.wola.internal.natv.WOLANativeUtils;
import com.ibm.wsspi.channelfw.ChannelFactory;

/**
 * WOLA Channel Factory Provider to hook this channel's factories with the framework.
 */
@Component(name = "com.ibm.ws.zos.channel.wola.factory", configurationPolicy = ConfigurationPolicy.IGNORE, immediate = true, property = { "service.vendor=IBM",
                                                                                                                                          "type:String=WOLAChannel" })
public class WOLAChannelFactoryProvider implements ChannelFactoryProvider {

    /**
     * A static reference to the OSGi-created instance of this class. This is how
     * WOLAChannel gets a reference to the instance. WOLAChannel's lifecycle
     * is managed by CFW, not OSGi, so there's no easy way to inject this reference.
     */
    private static WOLAChannelFactoryProvider instance;

    /**
     * Factories provided by this class.
     */
    private final Map<String, Class<? extends ChannelFactory>> factories = new HashMap<String, Class<? extends ChannelFactory>>();

    /**
     * DS ref.
     */
    private WOLANativeUtils wolaNativeUtils;

    /**
     * DS ref.
     */
    private WOLAConfig wolaConfig;

    /**
     * DS ref
     */
    private WolaOutboundConnMgr wolaOutboundConnMgr;

    /**
     * DS ref
     */
    private WOLAJNDICache wolaJndiCache;

    /**
     * DS ref
     */
    private EventEngine eventEngine;

    /**
     * DS refs. List of WOLA preInvoke/postInvoke interceptors (e.g security)
     */
    private final Set<WolaRequestInterceptor> wolaRequestInterceptors = new HashSet<WolaRequestInterceptor>();

    /**
     * DS ref
     */
    private ExecutorService executorService;

    private boolean attachedToWolaGroupSharedMemoryArea;

    private final RequestCountObject requestCountObject = new RequestCountObject();

    /**
     * Constructor.
     */
    public WOLAChannelFactoryProvider() {
        this.factories.put("WOLAChannel", WOLAChannelFactory.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Class<? extends ChannelFactory>> getTypes() {
        return this.factories;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
    }

    /**
     * DS method.
     *
     * Set the static instance.
     */
    @Activate
    protected void activate() {
        WOLAChannelFactoryProvider.instance = this;

    }

    /**
     * DS method.
     *
     * The component is shutting dow. Unset the static instance. This will cause
     * NPEs in any code that attempts to obtain and use the static instance.
     */
    @Deactivate
    protected void deactivate() {
        WOLAChannelFactoryProvider.instance = null;
    }

    /**
     * @return The static instance of this DS component.
     */
    protected static WOLAChannelFactoryProvider getInstance() {
        return WOLAChannelFactoryProvider.instance;
    }

    /**
     * Set DS ref.
     */
    @Reference
    protected void setEventEngine(EventEngine eventEngine) {
        this.eventEngine = eventEngine;
    }

    /**
     * Un-Set DS ref.
     */
    protected void unsetEventEngine(EventEngine eventEngine) {
        if (this.eventEngine == eventEngine) {
            this.eventEngine = null;
        }
    }

    /**
     * @return EventEngine DS ref.
     */
    protected EventEngine getEventEngine() {
        return eventEngine;
    }

    /**
     * Set DS ref.
     */
    @Reference
    protected void setWolaNativeUtils(WOLANativeUtils wolaNativeUtils) {
        this.wolaNativeUtils = wolaNativeUtils;
    }

    /**
     * Un-Set DS ref.
     */
    protected void unsetWolaNativeUtils(WOLANativeUtils wolaNativeUtils) {
        if (this.wolaNativeUtils == wolaNativeUtils) {
            this.wolaNativeUtils = null;
        }
    }

    /**
     * @return WOLANativeUtils DS ref.
     */
    protected WOLANativeUtils getWOLANativeUtils() {
        return wolaNativeUtils;
    }

    /**
     * Set DS ref.
     */
    @Reference
    protected void setWolaJndiCache(WOLAJNDICache wolaJndiCache) {
        this.wolaJndiCache = wolaJndiCache;
    }

    /**
     * Un-Set DS ref.
     */
    protected void unsetWolaJndiCache(WOLAJNDICache wolaJndiCache) {
        if (this.wolaJndiCache == wolaJndiCache) {
            this.wolaJndiCache = null;
        }
    }

    /**
     * @return WOLAJNDICache DS ref.
     */
    protected WOLAJNDICache getWolaJndiCache() {
        return wolaJndiCache;
    }

    /**
     * Set DS ref.
     */
    @Reference
    protected void setWolaConfig(WOLAConfig wolaConfig) {
        this.wolaConfig = wolaConfig;
    }

    /**
     * Un-Set DS ref.
     */
    protected void unsetWolaConfig(WOLAConfig wolaConfig) {
        if (this.wolaConfig == wolaConfig) {
            this.wolaConfig = null;
        }
    }

    /**
     * @return WOLAConfig DS ref.
     */
    protected WOLAConfig getWOLAConfig() {
        return wolaConfig;
    }

    /**
     * Set DS ref.
     */
    @Reference
    protected void setWolaOutboundConnMgr(WolaOutboundConnMgr wolaOutboundConnMgr) {
        this.wolaOutboundConnMgr = wolaOutboundConnMgr;
    }

    /**
     * Un-Set DS ref.
     */
    protected void unsetWolaOutboundConnMgr(WolaOutboundConnMgr wolaOutboundConnMgr) {
        if (this.wolaOutboundConnMgr == wolaOutboundConnMgr) {
            this.wolaOutboundConnMgr = null;
        }
    }

    /**
     * @return WolaOutboundConnMgr DS ref.
     */
    protected WolaOutboundConnMgr getOutboundConnMgr() {
        return wolaOutboundConnMgr;
    }

    /**
     * Set DS ref.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setWolaRequestInterceptor(WolaRequestInterceptor wolaRequestInterceptor) {
        this.wolaRequestInterceptors.add(wolaRequestInterceptor);
    }

    /**
     * Set DS ref.
     */
    protected void unsetWolaRequestInterceptor(WolaRequestInterceptor wolaRequestInterceptor) {
        this.wolaRequestInterceptors.remove(wolaRequestInterceptor);
    }

    /**
     * @return the set of WolaRequestInterceptors
     */
    protected Set<WolaRequestInterceptor> getWolaRequestInterceptors() {
        return wolaRequestInterceptors;
    }

    /**
     * Set DS ref.
     */
    @Reference
    protected void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    protected void unsetExecutorService(ExecutorService executorService) {
        if (this.executorService == executorService) {
            this.executorService = null;
        }
    }

    protected ExecutorService getExecutorService() {
        return this.executorService;
    }

    protected void setAttachedToWolaGroupSharedMemoryArea(boolean attached) {
        this.attachedToWolaGroupSharedMemoryArea = attached;
    }

    protected boolean isAttachedToWolaGroupSharedMemoryArea() {
        return this.attachedToWolaGroupSharedMemoryArea;
    }

    class RequestCountObject {
        private int requestCount;

        private int waiting;

        private RequestCountObject() {
            requestCount = 0;
            waiting = 0;
        }
    }

    /**
     * Waits for requests to complete
     */
    protected boolean waitForRequestsToComplete() {
        synchronized (requestCountObject) {
            // See if any requests
            if (requestCountObject.requestCount != 0) {
                requestCountObject.waiting = 1;
            } else {
                return true;
            }
        }
        // Do not hold up stop forever.
        for (int count = 0; count < 35; count++) {
            if (requestCountObject.requestCount == 0) {
                return true;
            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException inEx) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Increment the number of requests
     */
    protected boolean incrementRequestCount() {
        synchronized (requestCountObject) {
            if (requestCountObject.waiting == 0) {
                requestCountObject.requestCount = requestCountObject.requestCount + 1;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Decrement number of requests
     */
    protected void decrementRequestCount() {
        synchronized (requestCountObject) {
            requestCountObject.requestCount = requestCountObject.requestCount - 1;
        }
    }

}
