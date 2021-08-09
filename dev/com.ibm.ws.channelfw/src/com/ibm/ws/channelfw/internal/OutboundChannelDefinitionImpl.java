/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * This class will be used to create outbound channel definitions based on
 * ones that are passed in and need to be modified.
 */
public class OutboundChannelDefinitionImpl implements OutboundChannelDefinition {

    /** Trace tool. */
    private static TraceComponent tc = Tr.register(OutboundChannelDefinitionImpl.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /** Class variables */
    private Class<?> factory = null;
    private Map<Object, Object> factoryProps = null;
    private Map<Object, Object> channelProps = null;

    /** Serialization ID string */
    private static final long serialVersionUID = -4159575078432063481L;

    /**
     * Constructor.
     * 
     * @param existingChanDef
     * @param newProps
     * @param overwriteExisting
     */
    public OutboundChannelDefinitionImpl(OutboundChannelDefinition existingChanDef, Map<Object, Object> newProps, boolean overwriteExisting) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "constructor");
        }

        // Copy the existing channel definition into this one.
        factory = existingChanDef.getOutboundFactory();
        factoryProps = existingChanDef.getOutboundFactoryProperties();
        channelProps = existingChanDef.getOutboundChannelProperties();

        // Check for null props and replace with empty map.
        if (factoryProps == null) {
            factoryProps = new HashMap<Object, Object>();
        }
        if (channelProps == null) {
            channelProps = new HashMap<Object, Object>();
        }

        // Ensure newProps is not null.
        if (newProps == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Null properties found");
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "constructor");
            }
            return;
        }

        // Loop through the set in new properties passed in.
        for (Entry<Object, Object> entry : newProps.entrySet()) {
            if (channelProps.containsKey(entry.getKey())) {
                // Found the key in the existing props. Determine if it can be
                // overwritten.
                if (overwriteExisting) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Object oldValue = channelProps.get(entry.getKey());
                        Tr.debug(tc, "Found existing property, " + entry.getKey() + ", value " + oldValue + " replacing with " + entry.getValue());
                    }
                    channelProps.put(entry.getKey(), entry.getValue());
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found existing property, " + entry.getKey() + ", but not overwriting");
                    }
                }
            } else {
                // Did not find the key so add it to the list.
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Adding property, " + entry.getKey() + ", value " + entry.getValue());
                }
                channelProps.put(entry.getKey(), entry.getValue());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "constructor");
        }
    }

    /**
     * Print the contents of the input outbound channel definition.
     * Utility method used in debug messages.
     * 
     * @param def
     *            outbound channel definition
     * @return String representation of input object.
     */
    public static String toString(OutboundChannelDefinition def) {
        StringBuilder sb = new StringBuilder();
        sb.append("OutboundChannelDefinition@").append(def.hashCode());
        sb.append(' ').append(def.getOutboundFactory().getName());
        sb.append(",[");
        Map<Object, Object> map = def.getOutboundFactoryProperties();
        if (map == null || map.isEmpty()) {
            sb.append("null]");
        } else {
            for (Entry<?, ?> entry : map.entrySet()) {
                sb.append(entry.getKey());
                sb.append('=');
                sb.append(entry.getValue());
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, ']');
        }
        sb.append(",[");
        map = def.getOutboundChannelProperties();
        if (map == null || map.isEmpty()) {
            sb.append("null]");
        } else {
            for (Entry<Object, Object> entry : map.entrySet()) {
                sb.append(entry.getKey());
                sb.append('=');
                sb.append(entry.getValue());
                sb.append(',');
            }
            sb.setCharAt(sb.length() - 1, ']');
        }
        return sb.toString();
    }

    /*
     * @see
     * com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundFactory()
     */
    @Override
    public Class<?> getOutboundFactory() {
        return this.factory;
    }

    /*
     * @seecom.ibm.websphere.channelfw.OutboundChannelDefinition#
     * getOutboundFactoryProperties()
     */
    @Override
    public Map<Object, Object> getOutboundFactoryProperties() {
        return this.factoryProps;
    }

    /*
     * @seecom.ibm.websphere.channelfw.OutboundChannelDefinition#
     * getOutboundChannelProperties()
     */
    @Override
    public Map<Object, Object> getOutboundChannelProperties() {
        return this.channelProps;
    }

}
