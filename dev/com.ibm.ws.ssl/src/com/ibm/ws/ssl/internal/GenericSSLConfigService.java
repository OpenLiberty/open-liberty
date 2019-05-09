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
package com.ibm.ws.ssl.internal;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Generic SSL config service used to get various SSL configuration objects in a common way
 */
public abstract class GenericSSLConfigService {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(GenericSSLConfigService.class);

    protected volatile Map<String, Object> config = null;
    private volatile Map<String, Object> myProps = null;

    protected void activate(String id, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "activate " + id, properties);
        }
        config = properties;
    }

    protected void deactivate(String id, int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "deactivate " + id + ", reason=" + reason);
        }
    }

    /**
     * DS method to modify this component.
     *
     * @param properties
     */
    protected void modified(String id, Map<String, Object> properties) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "modified " + id, properties);
        }

        config = properties;
        myProps = null;
    }

    /**
     * This method take the configuration properties and builds a
     * Map<String,String> set of properties. Any Boolean values
     * turned to Strings.
     */
    public Map<String, Object> getProperties() {
        Map<String, Object> localProps = myProps;
        if (localProps == null) {
            localProps = new HashMap<String, Object>();
            if (config != null && !config.isEmpty()) {
                for (Map.Entry<String, Object> entry : config.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        localProps.put(entry.getKey(), entry.getValue());
                        continue;
                    }
                    if (entry.getValue() instanceof Boolean) {
                        Boolean boolVal = (Boolean) entry.getValue();
                        localProps.put(entry.getKey(), boolVal.toString());
                        continue;
                    }
                    if (entry.getValue() instanceof Integer) {
                        Integer IntVal = (Integer) entry.getValue();
                        localProps.put(entry.getKey(), IntVal.toString());
                        continue;
                    }
                }
            }
            myProps = localProps;
        }
        return localProps;
    }
}
