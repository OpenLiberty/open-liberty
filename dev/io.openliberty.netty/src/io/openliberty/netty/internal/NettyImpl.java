/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.kernel.service.utils.MetatypeUtils;

import io.openliberty.netty.Netty;

/**
 * This class is the implementation of the configuration and runtime interface
 * for modifying the Channel Framework.
 */
public class NettyImpl implements Netty, FFDCSelfIntrospectable {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(NettyImpl.class, NettyConstants.BASE_TRACE_NAME, NettyConstants.BASE_BUNDLE);
    private static final String nl = System.getProperty("line.separator");

    /**
     * Map to contain all registered services.
     */
    private Map<Class<?>, Object> services = null;

    /** Property name for the missing config warning message delay */
    public static final String PROPERTY_MISSING_CONFIG_WARNING = "warningWaitTime";
    /** Alias used in metatype */
    public static final String PROPERTY_CONFIG_ALIAS = "netty";
    /** Custom property for timed delay before warning about missing config in milliseconds */
    private long missingConfigWarning = 10000L;

    /** Singleton instance of the framework */
    private volatile static NettyImpl singleton = null;

    /**
     * Constructor for the channel framework.
     */
    public NettyImpl() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "constructor");
        }
        this.services = new HashMap<Class<?>, Object>();

        singleton = this;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "constructor");
        }
    }

    /**
     * Access the singleton instance.
     *
     * @return ChannelFrameworkImpl
     */
    public static NettyImpl getRef() {
        if (null == singleton) {
            synchronized (NettyImpl.class) {
                if (null == singleton) {
                    new NettyImpl();
                }
            }
        }
        return singleton;
    }

    /**
     * Set the custom property for the delay before warning about missing
     * configuration values.
     *
     * @param value
     */
    public void setMissingConfigWarning(Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Setting missing config warning delay to [" + value + "]");
        }
        try {
            long num = MetatypeUtils.parseLong(PROPERTY_CONFIG_ALIAS, PROPERTY_MISSING_CONFIG_WARNING, value, missingConfigWarning);
            if (0L <= num) {
                this.missingConfigWarning = num;
            }
        } catch (NumberFormatException nfe) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Value is not a number");
            }
        }
    }

    /**
     * Process configuration information.
     *
     * @param config
     */
    public void updateConfig(Map<String, Object> config) {

        Object value = config.get(PROPERTY_MISSING_CONFIG_WARNING);
        if (null != value) {
            setMissingConfigWarning(value);
        }
    }

    /*
     * @see io.openliberty.netty#destroy()
     */
    @Override
    public synchronized void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    }

    /**
     * This method will remove and destroy all parts of the framework. It
     * is called by the channel service when it is destroyed.
     */
    public synchronized void clear() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "clear");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "clear");
        }
    }

    /**
     * Utility method to extract a string representing the contents of a map.
     * This is currently used by various methods as part of debug tracing.
     *
     * @param map
     * @return string representing contents of Map
     */
    public static String stringForMap(Map<Object, Object> map) {
        StringBuilder sbOutput = new StringBuilder();
        if (map == null) {
            sbOutput.append("\tNULL");
        } else {
            for (Entry<Object, Object> entry : map.entrySet()) {
                sbOutput.append('\t').append(entry).append('\n');
            }
        }
        return sbOutput.toString();
    }

    @Override
    public String[] introspectSelf() {
        return null;
    }

}
