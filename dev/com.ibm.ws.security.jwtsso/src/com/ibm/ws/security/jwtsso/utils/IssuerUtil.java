/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.management.DynamicMBean;

import com.ibm.websphere.kernel.server.ServerInfoMBean;

public class IssuerUtil {
    public static final String JWTSSO_CONTEXT_PATH = "/jwt/";

    public IssuerUtil() {

    }

    public String getResolvedHostAndPortUrl(DynamicMBean httpsendpointInfoMBean, DynamicMBean httpendpointInfoMBean,
            ServerInfoMBean serverInfoMBean, String uniqueId) {
        String hosturl = null;
        ;
        if (httpsendpointInfoMBean != null) {
            try {
                String host = resolveHost((String) httpsendpointInfoMBean.getAttribute("Host"), serverInfoMBean);
                int port = (Integer) httpsendpointInfoMBean.getAttribute("Port");
                hosturl = "https://" + host + ":" + port;
            } catch (Exception e) {

            }
        } else if (httpendpointInfoMBean != null) {
            try {
                String host = resolveHost((String) httpendpointInfoMBean.getAttribute("Host"), serverInfoMBean);
                int port = (Integer) httpendpointInfoMBean.getAttribute("Port");
                hosturl = "http://" + host + ":" + port;
            } catch (Exception e) {

            }
        }

        if (hosturl != null) {
            hosturl = hosturl + JWTSSO_CONTEXT_PATH + uniqueId;
        }
        return hosturl;
    }

    protected String resolveHost(String host, ServerInfoMBean serverInfoMBean) {
        if ("*".equals(host)) {
            // Check configured ${defaultHostName}
            if (serverInfoMBean != null) {
                host = serverInfoMBean.getDefaultHostname();
                if (host == null || host.equals("localhost")) {
                    // This is, as a default, not useful. Use the local IP
                    // address instead.
                    host = getLocalHostIpAddress();
                }
            } else {
                host = getLocalHostIpAddress();
            }

        }
        return (host == null || host.trim().isEmpty()) ? "localhost" : host;
    }

    /**
     * @return InetAddress.getLocalHost().getHostAddress(); or null if that
     *         fails.
     */
    protected String getLocalHostIpAddress() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws UnknownHostException {
                    return InetAddress.getLocalHost().getHostAddress();
                }
            });

        } catch (PrivilegedActionException pae) {
            // FFDC it
            return null;
        }
    }

}
