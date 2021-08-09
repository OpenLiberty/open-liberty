/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal;

import java.io.NotSerializableException;
import java.math.BigInteger;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.channelfw.CFEndPoint;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Utility class for converting between CFEndPoint objects and UTF-8
 * encoded XML strings.
 */
public class CFEndPointSerializer {

    /** Trace tool. */
    private static final TraceComponent tc = Tr.register(CFEndPointSerializer.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Private constructor.
     */
    private CFEndPointSerializer() {
        // static class with no public constructor
    }

    /**
     * Determine the type of the Object passed in and add the XML format
     * for the result.
     * 
     * @param type
     * @param name
     * @param o
     * @return StringBuilder
     */
    static private StringBuilder determineType(String name, Object o) {
        String value = null;
        if (o instanceof String || o instanceof StringBuffer || o instanceof java.nio.CharBuffer || o instanceof Integer || o instanceof Long || o instanceof Byte
            || o instanceof Double || o instanceof Float || o instanceof Short || o instanceof BigInteger || o instanceof java.math.BigDecimal) {
            value = o.toString();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Skipping class: " + o.getClass());
            }
            return null;
        }

        // type="class" name="o"
        StringBuilder buffer = new StringBuilder(48);
        buffer.append(name);
        buffer.append("type=\"");
        // charbuffer is abstract so we might get HeapCharBuffer here, force it
        // to the generic layer in the XML output
        if (o instanceof java.nio.CharBuffer) {
            buffer.append("java.nio.CharBuffer");
        } else {
            buffer.append(o.getClass().getName());
        }
        buffer.append("\" ");
        buffer.append(name);
        buffer.append("=\"");
        buffer.append(value);
        buffer.append("\"");
        return buffer;
    }

    /**
     * Method to serialize a given channel object into the overall output
     * buffer.
     * 
     * @param buffer
     * @param ocd
     * @param order
     * @return StringBuilder (expanded with new data)
     * @throws NotSerializableException
     */
    static private StringBuilder serializeChannel(StringBuilder buffer, OutboundChannelDefinition ocd, int order) throws NotSerializableException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Serializing channel: " + order + " " + ocd.getOutboundFactory().getName());
        }
        buffer.append("   <channel order=\"");
        buffer.append(order);
        buffer.append("\" factory=\"");
        buffer.append(ocd.getOutboundFactory().getName());
        buffer.append("\">\n");
        Map<Object, Object> props = ocd.getOutboundChannelProperties();
        if (null != props) {
            for (Entry<Object, Object> entry : props.entrySet()) {
                if (null == entry.getValue()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Property value [" + entry.getKey() + "] is null, " + ocd.toString());
                    }
                    throw new NotSerializableException("Property value for [" + entry.getKey() + "] is null");
                }
                // TODO should pass around the one big stringbuffer instead
                // of creating all these intermediate ones
                StringBuilder kBuff = determineType("key", entry.getKey());
                if (null != kBuff) {
                    StringBuilder vBuff = determineType("value", entry.getValue());
                    if (null != vBuff) {
                        buffer.append("      <property ");
                        buffer.append(kBuff);
                        buffer.append(" ");
                        buffer.append(vBuff);
                        buffer.append("/>\n");
                    }
                }
            }
        }
        buffer.append("   </channel>\n");
        return buffer;
    }

    /**
     * Method to serialize the given end point object into an XML string.
     * 
     * @param point
     * @return String
     * @throws NotSerializableException
     */
    static public String serialize(CFEndPoint point) throws NotSerializableException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "serialize");
        }
        if (null == point) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Null CFEndPoint input for serialization");
            }
            throw new NotSerializableException("Null input");
        }
        // start the end point with name/host/port
        StringBuilder buffer = new StringBuilder(512);
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Serializing endpoint: " + point.getName());
        }
        buffer.append("<cfendpoint name=\"");
        buffer.append(point.getName());
        buffer.append("\" host=\"");
        buffer.append(point.getAddress().getCanonicalHostName());
        buffer.append("\" port=\"");
        buffer.append(point.getPort());
        buffer.append("\" local=\"");
        buffer.append(point.isLocal());
        buffer.append("\" ssl=\"");
        buffer.append(point.isSSLEnabled());
        buffer.append("\">\n");
        // loop through each channel in the list
        int i = 0;
        for (OutboundChannelDefinition def : point.getOutboundChannelDefs()) {
            buffer = serializeChannel(buffer, def, i++);
        }
        buffer.append("</cfendpoint>");
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Serialized string: \n" + buffer.toString());
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "serialize");
        }
        return buffer.toString();
    }
}