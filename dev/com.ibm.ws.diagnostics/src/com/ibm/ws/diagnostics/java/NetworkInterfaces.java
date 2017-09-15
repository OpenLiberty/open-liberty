/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.diagnostics.java;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.List;

import com.ibm.wsspi.logging.Introspector;

/**
 * Diagnostic handler to capture information about the network interfaces
 * on the system.
 */
public class NetworkInterfaces implements Introspector {
    @Override
    public String getIntrospectorName() {
        return "NetworkInterfaces";
    }

    @Override
    public String getIntrospectorDescription() {
        return "Network interface information from java.net.NetworkInterface";
    }

    /**
     * Introspect all network interfaces for diagnostic information.
     * 
     * @param out the output stream to write diagnostics to
     */
    @Override
    public void introspect(final PrintWriter writer) throws Exception {
        // Put out a header before the information
        writer.println("Network Interface Information");
        writer.println("-----------------------------");

        // Extract the interface information inside a doPriv
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                @Override
                public Void run() throws IOException {
                    // Iterate over the system network interfaces
                    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                    while (interfaces.hasMoreElements()) {
                        getInterfaceInfo(interfaces.nextElement(), writer);
                    }
                    return null;
                }
            });
        } catch (PrivilegedActionException pae) {
            throw (Exception) pae.getCause();
        }

    }

    /**
     * Capture interface specific information and write it to the provided writer.
     * 
     * @param networkInterface the interface to introspect
     * @param writer the print writer to write information to
     */
    private void getInterfaceInfo(NetworkInterface networkInterface, PrintWriter out) throws IOException {
        final String indent = "    ";

        // Basic information from the interface
        out.println();
        out.append("Interface: ").append(networkInterface.getDisplayName()).println();
        out.append(indent).append("          loopback: ").append(Boolean.toString(networkInterface.isLoopback())).println();
        out.append(indent).append("               mtu: ").append(Integer.toString(networkInterface.getMTU())).println();
        out.append(indent).append("    point-to-point: ").append(Boolean.toString(networkInterface.isPointToPoint())).println();
        out.append(indent).append("supports multicast: ").append(Boolean.toString(networkInterface.supportsMulticast())).println();
        out.append(indent).append("                up: ").append(Boolean.toString(networkInterface.isUp())).println();
        out.append(indent).append("           virtual: ").append(Boolean.toString(networkInterface.isVirtual())).println();

        // Interface address information
        List<InterfaceAddress> intfAddresses = networkInterface.getInterfaceAddresses();
        for (int i = 0; i < intfAddresses.size(); i++) {
            out.append(indent).append("InterfaceAddress #").append(Integer.toString(i + 1)).append(": ").append(String.valueOf(intfAddresses.get(i))).println();
        }

        // Network interface information
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        for (int i = 1; inetAddresses.hasMoreElements(); i++) {
            InetAddress inetAddress = inetAddresses.nextElement();
            out.append(indent).append("InetAddress #").append(Integer.toString(i)).println(":");
            out.append(indent).append(indent).append("    IP address: ").append(inetAddress.getHostAddress()).println();
            out.append(indent).append(indent).append("     host name: ").append(inetAddress.getHostName()).println();
            out.append(indent).append(indent).append("FQDN host name: ").append(inetAddress.getCanonicalHostName()).println();
        }
    }
}
