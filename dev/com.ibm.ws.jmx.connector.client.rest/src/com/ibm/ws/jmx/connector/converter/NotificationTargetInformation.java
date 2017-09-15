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
package com.ibm.ws.jmx.connector.converter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.management.ObjectName;

import com.ibm.ws.jmx.connector.client.rest.ClientProvider;

/**
 * Routing information for a JMX Notification which helps the connector
 * to correlate a Notification with the appropriate NotificationListener.
 * Combined with the ObjectName, the routing information provides a unique
 * identifier for an MBean in a cluster of servers.
 */
public final class NotificationTargetInformation {

    private ObjectName name;
    private final String nameAsString;
    private final Map<String, Object> routingInfo;

    public NotificationTargetInformation(ObjectName name) {
        this(name.getCanonicalName());
        this.name = name;
    }

    public NotificationTargetInformation(String name) {
        this.nameAsString = name;
        this.routingInfo = null;
    }

    public NotificationTargetInformation(ObjectName name, String hostName, String serverName, String serverUserDir) {
        this(name.getCanonicalName(), hostName, serverName, serverUserDir);
        this.name = name;
    }

    public NotificationTargetInformation(String name, String hostName, String serverName, String serverUserDir) {
        Map<String, Object> routingInfo = new HashMap<String, Object>();
        routingInfo.put(ClientProvider.ROUTING_KEY_HOST_NAME, hostName);
        routingInfo.put(ClientProvider.ROUTING_KEY_SERVER_NAME, serverName);
        routingInfo.put(ClientProvider.ROUTING_KEY_SERVER_USER_DIR, serverUserDir);
        this.nameAsString = name;
        this.routingInfo = Collections.unmodifiableMap(routingInfo);
    }

    public NotificationTargetInformation(ObjectName name, Map<String, Object> routingInfo) {
        this(name.getCanonicalName(), routingInfo);
        this.name = name;
    }

    public NotificationTargetInformation(String name, Map<String, Object> routingInfo) {
        this.nameAsString = name;
        this.routingInfo = (routingInfo != null) ? Collections.unmodifiableMap(new HashMap<String, Object>(routingInfo)) : null;
    }

    // Returns null if this object was not constructed with an ObjectName.
    public ObjectName getName() {
        return name;
    }

    public String getNameAsString() {
        return nameAsString;
    }

    public Map<String, Object> getRoutingInformation() {
        return routingInfo;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof NotificationTargetInformation)) {
            return false;
        }
        NotificationTargetInformation other = (NotificationTargetInformation) obj;
        return (nameAsString == other.nameAsString || (nameAsString != null && nameAsString.equals(other.nameAsString))) &&
               (routingInfo == other.routingInfo || (routingInfo != null && routingInfo.equals(other.routingInfo)));
    }

    @Override
    public int hashCode() {
        if (routingInfo != null) {
            int hash = routingInfo.hashCode() * 37;
            if (nameAsString != null) {
                hash += nameAsString.hashCode();
            }
            return hash;
        } else if (nameAsString != null) {
            return nameAsString.hashCode();
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ObjectName: ");
        sb.append(nameAsString);
        if (routingInfo != null) {
            sb.append(", RoutingInfo: ");
            sb.append(routingInfo);
        }
        sb.append(']');
        return sb.toString();
    }
}