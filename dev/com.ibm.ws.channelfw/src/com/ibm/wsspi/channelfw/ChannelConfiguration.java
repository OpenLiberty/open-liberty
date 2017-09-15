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
package com.ibm.wsspi.channelfw;

import java.util.Collections;
import java.util.Map;

import org.osgi.framework.Constants;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;

/**
 *
 */
public class ChannelConfiguration {

    /** Trace service */
    private static final TraceComponent tc =
                    Tr.register(ChannelConfiguration.class,
                                ChannelFrameworkConstants.BASE_TRACE_NAME,
                                ChannelFrameworkConstants.BASE_BUNDLE);

    private volatile Map<String, Object> config = null;

    protected void activate(Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Activating " + config.get(Constants.SERVICE_PID), config);
        }
        this.config = Collections.unmodifiableMap(config);
    }

    protected void deactivate(Map<String, Object> config, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Deactivating " + config.get(Constants.SERVICE_PID) + ", reason=" + reason, config);
        }
    }

    protected void modified(Map<String, Object> config) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Modified " + config.get(Constants.SERVICE_PID), config);
        }
        this.config = Collections.unmodifiableMap(config);
    }

    public Map<String, Object> getConfiguration() {
        return config;
    }

    public Object getProperty(String key) {
        Map<String, Object> map = config;

        return map == null ? null : map.get(key);
    }

    @Override
    public String toString() {
        return config.toString();
    }
}
