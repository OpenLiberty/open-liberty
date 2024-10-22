/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.crypto.certificateutil;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 *
 */
public class DefaultSubjectDN {

    private final String subjectDN;

    /**
     * Creates the default SubjectDN.
     */
    public DefaultSubjectDN() {
        this(null, null);
    }

    /**
     * Create the default SubjectDN based on the host and server names.
     *
     * @param hostName May be {@code null}. If {@code null} an attempt is made to determine it.
     * @param serverName May be {@code null}.
     */
    public DefaultSubjectDN(String hostName, String serverName) {
        if (hostName == null) {
            hostName = getHostName();
        }
        if (serverName == null) {
            subjectDN = "CN=" + hostName;
        } else {
            subjectDN = "CN=" + hostName + ",OU=" + serverName;
        }
    }

    /**
     * @return String the default SubjectDN.
     */
    public String getSubjectDN() {
        return subjectDN;
    }

    /**
     *  This method builds subjectAltNames ip addresses from System's configured network interface
     **/
    public static String buildSanIpStringFromNetworkInterface() {
        List<String> sanIpAddress = new ArrayList<String>();
        try {
            Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while(e.hasMoreElements()) {
                NetworkInterface n = e.nextElement();
                Enumeration<InetAddress> ee = n.getInetAddresses();
                while (ee.hasMoreElements()) {
                    InetAddress ip = ee.nextElement();
                    if(ip instanceof java.net.Inet4Address){
                        sanIpAddress.add(ip.getHostAddress());
                    }
                }
            }
        } catch (IOException e) {
            // no network interfaces found for the system
            sanIpAddress.add("127.0.0.1");
        }

        // on Liberty consider using String.join(",", sanIpAddress); instead of below. (twas doesn't compile with String.join())
        if (!sanIpAddress.isEmpty()) {
            StringBuilder sb = new StringBuilder("ip:" + sanIpAddress.get(0).toString());
            for (int i = 1; i < sanIpAddress.size(); i++) {
                sb.append(",ip:");
                sb.append(sanIpAddress.get(i).toString());
            }
            return sb.toString();
        }
        return null;
    }

    /**
     * Get the host name.
     *
     * @return String value of the host name or "localhost" if not able to resolve
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getCanonicalHostName();
        } catch (java.net.UnknownHostException e) {
            return "localhost";
        }
    }

}
