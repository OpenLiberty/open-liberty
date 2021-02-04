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
package com.ibm.ws.channelfw.internal;

import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;
import com.ibm.wsspi.channelfw.ChannelFramework;

/**
 * This class is the implementation of the configuration and runtime interface
 * for modifying the Channel Framework.
 */
public class ChannelFrameworkImpl implements ChannelFramework, FFDCSelfIntrospectable {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(ChannelFrameworkImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);
    private static final String nl = System.getProperty("line.separator");

    /** Singleton instance of the framework */
    private volatile static ChannelFrameworkImpl singleton = null;

    /**
     * Constructor for the channel framework.
     */
    public ChannelFrameworkImpl() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "constructor");
        }

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
    public static ChannelFrameworkImpl getRef() {
        if (null == singleton) {
            synchronized (ChannelFrameworkImpl.class) {
                if (null == singleton) {
                    new ChannelFrameworkImpl();
                }
            }
        }
        return singleton;
    }

    @Override
    public String[] introspectSelf() {
        return new String[] { this.toString() };
    }

    /**
     *
     */
    public void destroy() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "destroy");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "destroy");
        }
    }

    /**
     * @param cfwConfiguration
     */
    public void updateConfig(Map<String, Object> cfwConfiguration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "updateConfig");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "updateConfig");
        }

    }

}