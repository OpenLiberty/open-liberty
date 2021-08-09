/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.internal;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager;
import com.ibm.wsspi.bytebuffer.WsByteBufferPoolManager.DirectByteBufferHelper;

/**
 *
 */
@Component(service = ByteBufferConfiguration.class, name = "com.ibm.ws.bytebuffer", configurationPid = "com.ibm.ws.bytebuffer", configurationPolicy = ConfigurationPolicy.OPTIONAL,
           property = { "service.vendor=IBM" })
public class ByteBufferConfiguration {
    /** Trace service */
    private static final TraceComponent tc =
                    Tr.register(ByteBufferConfiguration.class,
                                MessageConstants.WSBB_TRACE_NAME,
                                MessageConstants.WSBB_BUNDLE);

    /** Reference to the pool manager, only create once */
    private volatile WsByteBufferPoolManager wsbbmgr = null;

    @Activate
    protected void activate(Map<String, Object> configuration) {
        modified(configuration);
    }

    @Deactivate
    protected void deactivate() {
        // TODO shouldn't there be a pool manager cleanup step?
        this.wsbbmgr = null;
    }

    private final AtomicReference<DirectByteBufferHelper> directByteBufferHelper = new AtomicReference<DirectByteBufferHelper>();

    @Reference(name="directByteBufferHelper", service=DirectByteBufferHelper.class, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setDirectByteBufferHelper(DirectByteBufferHelper helper) {
        this.directByteBufferHelper.set(helper);
    }

    protected void unsetDirectByteBufferHelper(DirectByteBufferHelper helper) {
        this.directByteBufferHelper.compareAndSet(helper, null);
    }

    public WsByteBufferPoolManager getBufferManager() {
        return wsbbmgr;
    }

    public synchronized void modified(Map<String, Object> newConfig) {
        if (null == newConfig) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Processing byte buffer config", newConfig);
        }

        if (null == this.wsbbmgr) {
            this.wsbbmgr = createBufferManager(newConfig);
        } else {
            updateBufferManager(newConfig);
        }
    }

    /**
     * Create the WSBB pool manager using the input configuration.
     * 
     * @param config
     * @return WsByteBufferPoolManager
     */
    private WsByteBufferPoolManager createBufferManager(Map<String, Object> config) {
        WsBBConfigException configError = null;
        Object oClass = config.get("class");
        if (null != oClass && oClass instanceof String) {
            // bundle is specifying a custom class
            String className = (String) oClass;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Pool class: " + className);
            }
            config.remove("class");
            Class<?> clazz = null;
            try {
                // ClassLoader cl = Thread.currentThread().getContextClassLoader();
                ClassLoader cl = ByteBufferConfiguration.class.getClassLoader();
                if (null != cl) {
                    clazz = cl.loadClass(className);
                }
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Class load failed using loader: " + e);
                }
                try {
                    clazz = Class.forName(className,
                                          true,
                                          Thread.currentThread().getContextClassLoader());
                } catch (Exception e2) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Class load failed using Class.forName: " + e2);
                    }
                }
            }

            if (null != clazz) {
                try {
                    Constructor<?> c = clazz.getConstructor(new Class[] { Map.class });
                    return (WsByteBufferPoolManager) c.newInstance(new Object[] { config });
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "New instance of class " + clazz + " failed: " + e.getCause());
                    }
                    if (e.getCause() instanceof WsBBConfigException) {
                        configError = (WsBBConfigException) e.getCause();
                    }
                }
            }
        }

        // try the default class with the configuration (assuming that
        // config did not previously fail)
        if (null == configError) {
            try {
                return new WsByteBufferPoolManagerImpl(directByteBufferHelper, config);
            } catch (WsBBConfigException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                    Tr.event(this, tc, "Error in configuration: " + e);
                }
            }
        }
        // if we failed to create the manager by now, it was due to bad
        // configuration
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Using default pool class with default config");
        }
        try {
            return new WsByteBufferPoolManagerImpl(directByteBufferHelper);
        } catch (WsBBConfigException e) {
            // shouldn't be possible with default config...
            FFDCFilter.processException(e, getClass().getName() + ".createBufferManager", "2");
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(this, tc, "Failure to create buffer manager: " + e);
            }
            return null;
        }
    }

    /**
     * This is used to provide the runtime configuration changes to an
     * existing pool manager, which is a small subset of the possible
     * creation properties.
     * 
     * @param properties
     */
    private void updateBufferManager(Map<String, Object> properties) {
        if (properties.isEmpty()) {
            return;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Ignoring runtime changes to WSBB config; " + properties);
        }
        // TODO: should be able to flip leak detection on or off
    }
}
