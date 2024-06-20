/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.channel.local.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.ws.zos.channel.local.queuing.LocalChannelProvider;
import com.ibm.wsspi.channelfw.ChannelFactory;

/**
 * This class is injected into the CHFWBundle in order to 
 * provide Channel Framework with a factory for creating
 * LocalComm channels.
 * 
 * This class maintains a static reference to itself, so that LocalCommChannel
 * can find and obtain a ref to it.
 */
@Component(configurationPolicy=ConfigurationPolicy.IGNORE, property={"service.vendor=IBM", "type:String=LocalCommChannel"})
public class LocalCommChannelFactoryProvider implements ChannelFactoryProvider {

    /** Factories provided by this class */
    private final Map<String, Class<? extends ChannelFactory>> factories;
    
    /**
     * A static reference to the OSGi-created instance of this class. This is how
     * LocalCommChannel gets a reference to the instance.  The LocalCommChannel's lifecycle
     * is managed by CFW, not OSGi, so there's no easy way to inject this reference.
     * LocalCommChannel could use a ServiceTracker or something like that, but that's
     * only mildly less hack-ish than this (and this is much easier).
     */
    private static LocalCommChannelFactoryProvider instance = null;
    
    /**
     * A reference to the native side of the local comm channel. 
     */
    @Reference 
    LocalChannelProvider localChannelProvider;
    
    /**
     * ExecutorService reference.
     */
    @Reference
    ExecutorService executorService;
    
	/**
     * Constructor.
     */
    public LocalCommChannelFactoryProvider() {
        this.factories = new HashMap<String, Class<? extends ChannelFactory>>();
        this.factories.put("LocalCommChannel", LocalCommChannelFactory.class);
    }

    @Override
    public Map<String, Class<? extends ChannelFactory>> getTypes() {
        return Collections.unmodifiableMap(factories);
    }

    @Override
    public void init() {}
    
    /**
     * Set the static instance. 
     */
    @Activate
    protected void activate() {
        LocalCommChannelFactoryProvider.instance = this;
    }
    
    /**
     * The component is shutting dow. Unset the static instance.  This will cause
     * NPEs in any code that attempts to obtain and use the static instance.
     */
    @Deactivate
    protected void deactivate() {
        LocalCommChannelFactoryProvider.instance = null;
    }
    
    /**
     * @return The static instance of this DS component.
     */
    protected static LocalCommChannelFactoryProvider getInstance() {
        return LocalCommChannelFactoryProvider.instance;
    }
	
	/**
	 * @return The LocalChannelProvider DS component (the native side of the local comm channel).
	 */
	protected LocalChannelProvider getLocalChannelProvider() {
	    return localChannelProvider;
	}
    
    /**
     * @return The ExecutorService DS component.
     */
    protected ExecutorService getExecutorService() {
        return executorService;
    }
    
}
