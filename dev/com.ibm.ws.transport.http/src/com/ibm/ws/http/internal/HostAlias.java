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
package com.ibm.ws.http.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.websphere.ras.Tr;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Helper class: common code to split a host:alias string apart, given special
 * considerations for IPv6 addresses.
 */
class HostAlias {
    final String hostName;
    final String portString;
    int port;
    final boolean isValid;
    final static Pattern validPort = Pattern.compile("(.*):(\\d+)");

    HostAlias(String host, int port) {
        this.port = port;
        this.hostName = host;
        this.portString = Integer.toString(port);
        this.isValid = true;
    }

    @FFDCIgnore(URISyntaxException.class)
    HostAlias(String alias, String virtualHostName) {
        String port = HttpServiceConstants.DEFAULT_PORT;
        String host = "";
        boolean valid = true;
        try {
            int pos = alias.lastIndexOf(':');
            if (pos < 0 || alias.endsWith("]")) {
                host = alias;
            } else {
                Matcher portMatch = validPort.matcher(alias);
                if (portMatch.matches()) {
                    host = portMatch.group(1);
                    port = portMatch.group(2);
                } else {
                    throw new URISyntaxException(alias, Tr.formatMessage(VirtualHostMap.tc, "badHostPortReason"));
                }
            }

            if (host.contains(HttpServiceConstants.WILDCARD)) {
                if (host.length() > 1)
                    throw new URISyntaxException(alias, Tr.formatMessage(VirtualHostMap.tc, "wildcardReason"));
            } else {
                // If there is an error parsing the URI (handles IP addresses of both types
                // with or without the []), then the host will be null and the port -1
                URI uri = new URI("http://" + host);
                host = uri.getHost();
                if (host == null) {
                    throw new URISyntaxException(alias, Tr.formatMessage(VirtualHostMap.tc, "badHostPortReason"));
                }
            }
        } catch (URISyntaxException ex) {
            Tr.warning(VirtualHostMap.tc, "invalidAlias", virtualHostName, ex.getMessage());
            valid = false;
        }

        this.hostName = host == null ? "" : host.toLowerCase(Locale.ENGLISH);
        this.portString = port;
        this.port = Integer.valueOf(port);
        this.isValid = valid;
    }

    @Override
    public String toString() {
        return hostName + ':' + portString;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((hostName == null) ? 0 : hostName.hashCode());
        result = prime * result + ((portString == null) ? 0 : portString.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HostAlias other = (HostAlias) obj;
        if (hostName == null) {
            if (other.hostName != null)
                return false;
        } else if (!hostName.equals(other.hostName))
            return false;
        if (portString == null) {
            if (other.portString != null)
                return false;
        } else if (!portString.equals(other.portString))
            return false;
        return true;
    }

    public static List<String> toStringList(Collection<HostAlias> haList) {
        if (haList == null)
            return Collections.emptyList();

        ArrayList<String> list = new ArrayList<String>(haList.size());
        for (HostAlias ha : haList)
            list.add(ha.toString());
        return list;
    }
}
