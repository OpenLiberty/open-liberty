/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.channelfw.utils;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Utility methods for finding and resolving the host name of this server.
 */
public class HostNameUtils {

    /** Trace tool. */
    private static final TraceComponent tc = Tr.register(HostNameUtils.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    public static final String LOCALHOST = "localhost";
    public static final String WILDCARD = "*";

    private static final boolean PREFER_IPV6 = Boolean.getBoolean("java.net.preferIPv6Addresses");

    /**
     * Try to determine the hostname of this server, one that is suitable for use in
     * URLs that point to this server. If resolving null or '*', this method will use the setting
     * of the java.net.preferIPv6Addresses system property to determine which type of address
     * to use (if possible).
     * 
     * @param hostToResolve The hostname to resolve, may be null or '*'
     * @return a reachable/remote hostname or IP address, or null if the hostname could not be resolved.
     * @see #tryResolveHostName(String, boolean)
     */
    @Trivial
    public static String tryResolveHostName(String nameToResolve) {
        return tryResolveHostName(nameToResolve, PREFER_IPV6);
    }

    /**
     * Try to determine the hostname of this server, one that is suitable for use in
     * URLs that point to this server. If resolving null or '*', this method will use the setting
     * of the preferIPv6 parameter to determine which type of address to return (if possible).
     * <p>
     * This method does more than the typical <code>InetAddress.getLocalHost().getCanonicalHostName()</code>.
     * <p>
     * If a hostname is provided, it will attempt to resolve that hostname, and then will ensure that
     * the resulting {@link InetAddress} is associated with a known {@link NetworkInterface}.
     * <p>
     * If a hostname is not specified (either null or '*'), this method will attempt to discover
     * a hostname to use based on available {@link NetworkInterface}s. It will look only at non-loopback,
     * non-virtual interfaces, and will prefer those that are not point-to-point (but will fall back to
     * a point-to-point interface if nothing else can be found). The IP protocol version preferences
     * will also be taken into account when trying to find a usable address.
     * 
     * @param hostToResolve The hostname to resolve, may be null or '*'
     * @param preferIPv6 If true, when trying to resolve a host for '*', it will prefer IPv6 addresses.
     * @return a reachable/remote hostname or IP address, or null if the hostname could not be resolved.
     */
    public static String tryResolveHostName(final String nameToResolve, final boolean preferIPv6) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Trying to resolve hostname : " + nameToResolve + ", prefer IPv6 : " + preferIPv6);
        }

        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            @FFDCIgnore({ SocketException.class, UnknownHostException.class })
            public String run() {
                InetAddress resultAddr = null;

                if (LOCALHOST.equalsIgnoreCase(nameToResolve)) {
                    return LOCALHOST;
                } else if (WILDCARD.equals(nameToResolve) || nameToResolve == null || nameToResolve.isEmpty()) {
                    InetAddressResult result = new InetAddressResult(preferIPv6); // preferred and fallback

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Result : " + result);
                        if (result != null) {
                            Tr.debug(tc, "Address : " + result.getAddress() + ", needs preferred address : " + result.needsPreferredAddr());
                        }
                    }

                    List<NetworkInterface> p2p = new ArrayList<NetworkInterface>();

                    try {
                        // '*', so let's pick the first interface that is not
                        // a loopback, or virtual interface, and use that if we can.
                        // NOTE: findPreferredAddress excludes link local addresses
                        Enumeration<NetworkInterface> nicEnum = NetworkInterface.getNetworkInterfaces();
                        for (NetworkInterface nic : Collections.list(nicEnum)) {

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "NIC : " + nic);
                                Tr.debug(tc, "isUp : " + nic.isUp());
                                Tr.debug(tc, "isVirtual : " + nic.isVirtual());
                                Tr.debug(tc, "isLoopback : " + nic.isLoopback());
                                Tr.debug(tc, "isPointToPoint : " + nic.isPointToPoint());
                            }

                            Enumeration<InetAddress> addrs = nic.getInetAddresses();

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                for (InetAddress addr : Collections.list(addrs)) {
                                    Tr.debug(tc, "Address : " + addr + ", isLinkLocal : " + addr.isLinkLocalAddress());
                                }
                            }

                            if (nic.isUp() && !nic.isVirtual() && !nic.isLoopback()) {
                                if (nic.isPointToPoint()) {
                                    // remember p2p to try later.
                                    p2p.add(nic);
                                } else if (findPreferredAddress(nic, result, preferIPv6)) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Found preferred address");
                                    }
                                    break;
                                }
                            }
                        }

                        // If we haven't found our preferred protocol yet, try any p2p interfaces.
                        // This is required on z/OS (RTC Java Defect 125337), which for some reason has 
                        // many of it's network interfaces configured as point-to-point.
                        // NOTE: findPreferredAddress excludes link local addresses
                        if (result.needsPreferredAddr() && !p2p.isEmpty()) {
                            for (NetworkInterface nic : p2p) {
                                if (findPreferredAddress(nic, result, preferIPv6)) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                        Tr.debug(tc, "Found preferred address in point to point list");
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (SocketException e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception while finding preferred address : " + e);
                        }
                    }

                    // get the address we found (if there is one). The result class
                    // manages preferred vs. fallback protocol version
                    resultAddr = result.getAddress();
                } else {
                    // We were given a specific host name or IP address to verify.
                    resultAddr = findLocalHostAddress(nameToResolve, preferIPv6);
                }

                // Woo-hoo we found an address to use.
                if (resultAddr != null) {
                    String hostAddr = resultAddr.getHostAddress();
                    // preserve lower case: required for matching all kinds of things later.. 
                    String hostName = resultAddr.getCanonicalHostName().toLowerCase();

                    // If this is an IPv6 address, we need extra text to make it usable in messages
                    // IPv6 addresses can be compressed: ::1 vs. 0:0:0:0:0:0:0:1, some OS will use the long
                    // form as the canonicalized result when the short form was provided.
                    if (resultAddr instanceof Inet6Address && hostName.contains(":") && !hostName.startsWith("[")) {
                        hostName = "[" + hostAddr + "]";
                    }

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "tryResolveHostName returning : " + hostName);
                    }
                    return hostName;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "tryResolveHostName returning null");
                }

                // Bummer. return null to disambiguate resolved localhost from unresolvable address.
                // The caller may fallback to localhost in this case.
                return null;
            }

            /**
             * Look at the InetAddresses associated with a NetworkInterface. Ignoring LinkLocal addresses,
             * return at the first instance of an InetAddress of the preferred protocol, and
             * remember the first address found for the fallback protocol (if we get there).
             * 
             * @param nic NetworkInterface to inspect
             * @param result small inner class that stores results (most importantly the fallback address)
             * @param preferIPv6 if true, prefer IPv6 InetAddresses
             * @return true if we found an InetAddress for the preferred protocol, otherwise return false so we keep looking
             */
            private boolean findPreferredAddress(NetworkInterface nic, InetAddressResult result, boolean preferIPv6) {
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                for (InetAddress addr : Collections.list(addrs)) {
                    // skip linklocal addrs -- remember this is called when we're trying to 
                    // generate an address to use in URLs when listening on all interfaces.
                    // Link local is not always usable, so we'll avoid it, and allow the caller
                    // to fallback to localhost, which is more likely to work.
                    if (!addr.isLinkLocalAddress() && result.setAddressIsPreferred(addr))
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Verifies whether or not the provided hostname references
     * an interface on this machine. The provided name is unchanged by
     * this operation.
     * 
     * @param hostName
     * @return true if the hostname refers to a method on this interface
     *         false if not.
     */
    @Trivial
    public static boolean validLocalHostName(String hostName) {
        InetAddress addr = findLocalHostAddress(hostName, PREFER_IPV6);
        return addr != null;
    }

    /**
     * Verifies whether or not the provided hostname references
     * an interface on this machine. The provided name is unchanged by
     * this operation.
     * 
     * @param hostName
     * @return true if the hostname refers to a method on this interface
     *         false if not.
     */
    @Trivial
    public static boolean validLocalHostName(String hostName, boolean preferIPv6) {
        InetAddress addr = findLocalHostAddress(hostName, preferIPv6);
        return addr != null;
    }

    /**
     * Return an {@link InetAddress} that is associated with the loopback address
     * or with a {@link NetworkInterface} on this machine. This allows the use of
     * link-local addresses, etc.
     * 
     * @param hostName HostName to verify
     * @param preferIpV6
     * @return an {@link InetAddress} resolved from the given hostname that
     *         is a loopback address or is associated with a {@link NetworkInterface} on this machine.
     */
    private static InetAddress findLocalHostAddress(final String nameToResolve, final boolean preferIPv6) {
        return AccessController.doPrivileged(new PrivilegedAction<InetAddress>() {
            @Override
            @FFDCIgnore({ SocketException.class, UnknownHostException.class })
            public InetAddress run() {
                InetAddressResult result = new InetAddressResult(preferIPv6);
                try {
                    // We are checking a configured hostname or IP address. This could be a shortname, like "bob", 
                    // or could be a qualified name like 'was.pok.ibm.com'. If it is "bob", 
                    // there is a possibility of getting something else's ip address back
                    // with a blind getAllByName.
                    InetAddress[] addrs = InetAddress.getAllByName(nameToResolve);

                    if (addrs != null) {
                        // This loop makes sure that the host that was specified *can* be resolved back
                        // to a nic on this machine... we don't care why or how (ipv4 vs. ipv6)...
                        for (InetAddress addr : addrs) {
                            if (addr.isLoopbackAddress()) {
                                // On z/OS, 127.0.0.1 is not correctly matched to the loopback 
                                // network interface (the call below returns null, see RTC Java Defect 125337). 
                                // If the addr is a loopback, we know that it is an address local
                                // to this machine, so we can answer appropriately.
                                // Try to preserve the IP address preference, for getCanonicalHostName behavior.
                                if (result.setAddressIsPreferred(addr))
                                    break;
                            } else {
                                NetworkInterface nic = NetworkInterface.getByInetAddress(addr);
                                // We found a nic that is bound to the specified hostname/address,
                                // which means we can use that IP address. Try to find the address
                                // of the preferred protocol type
                                if (nic != null) {
                                    if (result.setAddressIsPreferred(addr))
                                        break;
                                }
                            }
                        }
                    }
                } catch (SocketException e) {
                } catch (UnknownHostException e) {
                }

                return result.getAddress();
            }
        });
    }

    private static class InetAddressResult {
        final boolean preferIPv6;
        InetAddress preferred;
        InetAddress fallback;

        InetAddressResult(boolean preferIPv6) {
            this.preferIPv6 = preferIPv6;
        }

        boolean needsPreferredAddr() {
            return preferred == null;
        }

        /**
         * @param addr address to add
         * @return true if the parameter is an InetAddress of the preferred protocol;
         */
        boolean setAddressIsPreferred(InetAddress addr) {
            if (preferIPv6) {
                if (addr instanceof Inet6Address) {
                    preferred = addr; // we'll pick this one.
                    return true;
                } else {
                    fallback = addr; // remember one of these
                }
            } else {
                if (addr instanceof Inet4Address) {
                    preferred = addr; // we'll pick this one.
                    return true;
                } else {
                    fallback = addr; // remember one of these
                }
            }

            return false;
        }

        InetAddress getAddress() {
            if (preferred != null)
                return preferred;
            return fallback;
        }

    }
}
