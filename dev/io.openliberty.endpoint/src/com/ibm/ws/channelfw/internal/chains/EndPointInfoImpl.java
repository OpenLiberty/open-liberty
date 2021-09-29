/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.internal.chains;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.AttributeChangeNotification;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationBroadcasterSupport;
import javax.management.StandardEmitterMBean;

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.endpoint.EndPointInfoMBean;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.endpoint.EndpointConstants;
/**
 * EndPointInfo is a dynamically updated MBean.
 * - The host binding and port value can be changed dynamically.
 * - The name of the EndPointInfo is unique and can not be changed dynamically.
 *
 * If the backing configuration for this EndPointInfo changes its
 * name, a new EndPointInfo object must be created.
 */
public class EndPointInfoImpl extends StandardEmitterMBean implements EndPointInfo {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(EndPointInfoImpl.class,
                                                         EndpointConstants.BASE_TRACE_NAME,
                                                         EndpointConstants.BASE_BUNDLE);

    /** Name of this endpoint */
    private final String name;

    /** Host for this endpoint - can be changed dynamically */
    private String host;

    /** Port for this endpoint - can be changed dynamically */
    private int port;

    /** Notification sequence counter */
    private final AtomicLong sequenceNum = new AtomicLong();

    /**
     * Validates the host name.
     * If the host name is invalid, an IllegalArgumentException is thrown.
     *
     * @param host The host name to validate
     * @throws IllegalArgumentException if the host name is null or empty
     */
    private void validateHostName(String host) {
        if (null == host || host.isEmpty()) {
            throw new IllegalArgumentException("Invalid host: " + host);
        }
    }

    /**
     * Constructor.
     * Package protected to avoid exposing the constructor in the MBean
     * interface and to enforce creation through {@link EndPointMgrImpl#defineEndPoint(String, String, int)}
     *
     * @param name
     * @param host
     * @param port
     * @throws IllegalArgumentException if name or host is empty
     */
    EndPointInfoImpl(String name, String host, int port) throws NotCompliantMBeanException {
        super(EndPointInfoMBean.class, true, new NotificationBroadcasterSupport((Executor) null, new MBeanNotificationInfo(new String[] { AttributeChangeNotification.ATTRIBUTE_CHANGE }, AttributeChangeNotification.class.getName(), "")));

        if (null == name || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        validateHostName(host);
        this.name = name;
        this.host = host;
        this.port = port;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Created: " + this);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public String getHost() {
        return this.host;
    }

    /**
     * Update the host value for this end point.
     * Will emit an AttributeChangeNotification if the value changed.
     *
     * @param newHost The new (or current) host name value. If no change, no notification is sent.
     * @return String The previous host value
     */
    public String updateHost(final String newHost) {
        validateHostName(newHost);

        String oldHost = this.host;
        this.host = newHost;

        if (!oldHost.equals(newHost)) {
            sendNotification(new AttributeChangeNotification(this, sequenceNum.incrementAndGet(), System.currentTimeMillis(), this.toString()
                                                                                                                              + " host value changed", "Host", "java.lang.String", oldHost, newHost));
        }

        return oldHost;
    }

    /** {@inheritDoc} */
    @Override
    public int getPort() {
        return this.port;
    }

    /**
     * Update the port value for this end point.
     * Will emit an AttributeChangeNotification if the value changed.
     *
     * @param newPort The new (or current) port value. If no change, no notification is sent.
     * @return int The previous port value
     */
    public int updatePort(final int newPort) {
        int oldPort = this.port;
        this.port = newPort;

        if (oldPort != newPort) {
            sendNotification(new AttributeChangeNotification(this, sequenceNum.incrementAndGet(), System.currentTimeMillis(), this.toString()
                                                                                                                              + " port value changed", "Port", "java.lang.Integer", oldPort, newPort));
        }

        return oldPort;
    }

    /** {@inheritDoc} */
    @Override
    protected final String getDescription(MBeanInfo info) {
        return "Informational MBean representing an active endpoint.";
    }

    /** {@inheritDoc} */
    @Override
    protected final String getDescription(MBeanAttributeInfo info) {
        String description = "Unknown attribute";

        if (info != null) {
            String operationName = info.getName();
            if (operationName != null) {
                if (operationName.equals("Name")) {
                    description = "Return the name of the endpoint.";
                } else if (operationName.equals("Host")) {
                    description = "Return the listening host name of the endpoint. A value of '*' means it is listening on all available host names.";
                } else if (operationName.equals("Port")) {
                    description = "Return the listening port of the endpoint.";
                }
            }
        }

        return description;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EndPoint ").append(getName()).append('=');
        sb.append(getHost()).append(':').append(getPort());
        return sb.toString();
    }

}
