/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.filter.FilterList;
import com.ibm.ws.http.channel.internal.filter.FilterListFastStr;
import com.ibm.ws.http.channel.internal.filter.FilterListSlowStr;
import com.ibm.ws.http.channel.internal.filter.FilterListStr;

/**
 * Parses and keeps track of the HttpDispatcher trustedHeaderOrigin and trustedSensitiveHeaderOrigin configurations
 * Adapted from com.ibm.ws.tcpchannel.internal.AccessLists
 */
public class TrustedHeaderOriginLists {

    private static final TraceComponent tc = Tr.register(TrustedHeaderOriginLists.class);

    private FilterList trustedAddresses = null;
    private FilterList sensitiveTrustedAddresses = null;
    private FilterListStr trustedHosts = null;
    private FilterListStr sensitiveTrustedHosts = null;
    private boolean trustAllOrigins = false;
    private boolean trustAllSensitiveOrigins = false;
    private boolean disableAllOrigins = false;
    private boolean disableAllSensitiveOrigins = false;

    protected TrustedHeaderOriginLists() {
    }

    /**
     * Parses addresses and hostnames from trustedHeaderOrigin and trustedSensitiveHeaderOrigin
     *
     * @param trustedPrivateHeaderHosts   String[] of hosts to trust for non-sensitive private headers
     * @param trustedSensitiveHeaderHosts String[] of hosts to trust for sensitive private headers
     */
    protected void parseTrustedPrivateHeaderOrigin(String[] trustedPrivateHeaderHosts, String[] trustedSensitiveHeaderHosts) {

        // Parse trustedHeaderOrigin.  The default value is * (any host)
        List<String> addrs = new ArrayList<String>();
        if (trustedPrivateHeaderHosts != null && trustedPrivateHeaderHosts.length > 0) {
            for (String host : trustedPrivateHeaderHosts) {
                if ("none".equalsIgnoreCase(host)) {
                    // if "none" is listed, private headers are not trusted on any host.
                    // however any hosts listed in trustedSensitiveHeaderOrigin can still send private headers
                    addrs.clear();
                    disableAllOrigins = true;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "trusted private headers hosts: none");
                    }
                    break;
                } else if ("*".equals(host)) {
                    // stop processing, empty the list, fall through to below.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "trusted private headers hosts: *");
                    }
                    trustAllOrigins = true;
                    addrs.clear();
                    break;
                } else {
                    addrs.add(host);
                }
            }
        } else {
            // no trusted header hosts were defined, use defualt - "*"
            trustAllOrigins = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "trusted private headers hosts: *");
            }
        }
        // if IP addresses were listed, only trust private headers from those hosts
        if (!addrs.isEmpty()) {
            HashSet<String> restrictPrivateHeaderOrigin = new HashSet<String>();
            for (String s : addrs) {
                if (s != null && !s.isEmpty()) {
                    restrictPrivateHeaderOrigin.add(s.toLowerCase());
                }
            }
            parseAndSetOrigins(restrictPrivateHeaderOrigin, false);
        }
        addrs.clear();

        // Parse trustedSensiveHeaderOrigin.  The default value is none (no hosts trusted)
        if (trustedSensitiveHeaderHosts != null && trustedSensitiveHeaderHosts.length > 0) {
            for (String host : trustedSensitiveHeaderHosts) {
                if ("none".equalsIgnoreCase(host)) {
                    // don't trust sensitive private headers from any host
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "trusted sensitive private headers hosts: none");
                    }
                    disableAllSensitiveOrigins = true;
                    return;
                } else if ("*".equals(host)) {
                    // sensitive private headers trusted from any host
                    addrs.clear();
                    trustAllSensitiveOrigins = true;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "trusted sensitive private headers hosts: *");
                    }
                    break;
                } else {
                    addrs.add(host);
                }
            }
        } else {
            // no trusted sensitive header hosts were defined, use defualt - "none"
            disableAllSensitiveOrigins = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "trusted sensitive private headers hosts: none");
            }
            return;
        }
        // if hosts were listed, only trust sensitive private headers from those hosts
        if (!addrs.isEmpty()) {
            HashSet<String> restrictSensitiveHeaderOrigin = new HashSet<String>();
            for (String s : addrs) {
                if (s != null && !s.isEmpty()) {
                    restrictSensitiveHeaderOrigin.add(s.toLowerCase());
                }
            }
            parseAndSetOrigins(restrictSensitiveHeaderOrigin, true);
        }
    }

    /**
     * Given a set of hosts, create the FilterList objects which will be used for trust checks
     *
     * @param origins
     * @param sensitive
     */
    private void parseAndSetOrigins(HashSet<String> origins, boolean sensitive) {
        List<String> addrs = new ArrayList<String>();
        List<String> hosts = new ArrayList<String>();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "parseAndSetOrigins sensitive=" + sensitive);
        }

        if (origins != null && !origins.isEmpty()) {
            for (String origin : origins) {
                boolean result = isStringIPAddressesValid(new String[] { origin });
                if (result) {
                    addrs.add(origin);
                } else {
                    result = isStringHostnameValid(origin);
                    if (!result) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                            if (!sensitive) {
                                Tr.warning(tc, "trusted.header.origin.invalid.value", new Object[] { origin });
                            } else {
                                Tr.warning(tc, "trusted.sensitive.header.origin.invalid.value", new Object[] { origin });
                            }
                        }
                        continue;
                    } else {
                        hosts.add(origin);
                    }
                }
            }
        }
        if (!sensitive) {
            trustedAddresses = new FilterList();
            if (addrs != null && !addrs.isEmpty()) {
                trustedAddresses.buildData(addrs.toArray(new String[0]), false);
                trustedAddresses.setActive(true);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "trusted private headers host addresses: " + Arrays.toString(addrs.toArray()));
                }
            }

            trustedHosts = new FilterListFastStr();
            if (hosts != null && !hosts.isEmpty()) {
                if (trustedHosts.buildData(hosts.toArray(new String[0])) == false) {
                    trustedHosts = new FilterListSlowStr();
                    trustedHosts.buildData(hosts.toArray(new String[0]));
                }
                trustedHosts.setActive(true);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "trusted private headers host names: " + Arrays.toString(addrs.toArray()));
                }
            }
        } else {
            sensitiveTrustedAddresses = new FilterList();
            if (addrs != null && !addrs.isEmpty()) {
                sensitiveTrustedAddresses.buildData(addrs.toArray(new String[0]), false);
                sensitiveTrustedAddresses.setActive(true);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "trusted sensitive private headers host addresses: " + Arrays.toString(addrs.toArray()));
                }

            }

            sensitiveTrustedHosts = new FilterListFastStr();
            if (hosts != null && !hosts.isEmpty()) {
                if (sensitiveTrustedHosts.buildData(hosts.toArray(new String[0])) == false) {
                    sensitiveTrustedHosts = new FilterListSlowStr();
                    sensitiveTrustedHosts.buildData(hosts.toArray(new String[0]));
                }
                sensitiveTrustedHosts.setActive(true);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "trusted sensitive private headers host names: " + Arrays.toString(hosts.toArray()));
                }
            }
        }
    }

    /**
     * Query whether a given client address should be trusted to send non-sensitive private headers
     *
     * @param InetAddress remoteAddr
     * @param isSensitive
     * @return true if the remote address is allowed to send private headers
     */
    protected boolean isTrusted(InetAddress remoteAddr, boolean isSensitive) {

        // return true if a "*" value is configured for the applicable list
        boolean isTrusted = (!isSensitive && trustAllOrigins) || trustAllSensitiveOrigins;

        if (!isTrusted) {
            if (remoteAddr != null) {
                if (sensitiveTrustedAddresses != null && sensitiveTrustedAddresses.getActive()) {
                    // check if the remote IP address is in the trustedHeaderSensitiveOrigin list
                    if (remoteAddr instanceof Inet6Address) {
                        isTrusted = sensitiveTrustedAddresses.findInList6(remoteAddr.getAddress());
                    } else {
                        isTrusted = sensitiveTrustedAddresses.findInList(remoteAddr.getAddress());
                    }
                }
                if (!isSensitive && !disableAllOrigins) {
                    // check if remote host is trusted to send non-sensitive private headers
                    if (!isTrusted && trustedAddresses != null && trustedAddresses.getActive()) {
                        // check if the remote IP address is in the trustedHeaderOrigin list
                        if (remoteAddr instanceof Inet6Address) {
                            isTrusted = trustedAddresses.findInList6(remoteAddr.getAddress());
                        } else {
                            isTrusted = trustedAddresses.findInList(remoteAddr.getAddress());
                        }
                    }
                    String hostname = null;
                    if (!isTrusted && sensitiveTrustedHosts != null && sensitiveTrustedHosts.getActive()) {
                        // check if the remote hostname is in the trustedHeaderSensitiveOrigin list
                        if ((hostname = getHostName(remoteAddr)) != null) {
                            hostname = hostname.toLowerCase();
                            isTrusted = sensitiveTrustedHosts.findInList(hostname);
                        }
                    }
                    if (!isTrusted && trustedHosts != null && trustedHosts.getActive()) {
                        // check if the remote hostname is in the trustedHeaderOrigin list
                        if (hostname != null || (hostname = getHostName(remoteAddr)) != null) {
                            hostname = hostname.toLowerCase();
                            isTrusted = trustedHosts.findInList(hostname);
                        }
                    }
                } else if (!disableAllSensitiveOrigins) {
                    // check if remote host is trusted to send sensitive private headers
                    if (!isTrusted && sensitiveTrustedHosts != null && sensitiveTrustedHosts.getActive()) {
                        // check if the remote hostname is in the trustedHeaderSensitiveOrigin list
                        String hostname = getHostName(remoteAddr);
                        if (hostname != null) {
                            hostname = hostname.toLowerCase();
                            isTrusted = sensitiveTrustedHosts.findInList(hostname);
                        }
                    }
                }
            } else {
                // remoteAddr is null, so return the defaults: true for non-sensitive (unless all non-sensitive are
                // explicitly disabled), and false for sensitive
                if (!isSensitive) {
                    isTrusted = !disableAllOrigins;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "isTrusted returning " + isTrusted + " for remote address = " + remoteAddr + " sensitive = " + isSensitive);
        }
        return isTrusted;
    }

    /**
     * Test the IP filter configuration values.
     *
     * @param value address name
     * @return true if the passed value represents a valid IP address
     */
    private boolean isStringIPAddressesValid(String[] value) {
        FilterList f = new FilterList();
        return f.buildData(value, true);
    }

    /**
     * Test the hostname configuration values.
     *
     * @param value hostname
     * @return boolean
     */
    private boolean isStringHostnameValid(String value) {

        // rip out a leading wildcard
        if (value.startsWith("*")) {
            value = value.substring(1);
        }
        if (value.length() > 0) {
            // validate the chars for the entire string
            for (int n = 0; n < value.length(); n++) {
                char c = value.charAt(n);
                int cint = c;
                // since this is a hostname, only '.', `-` '0'-'9', 'a'-'z', and 'A'-'Z' are valid
                if (!(cint == 45 || cint == 46 || (47 < c && c < 58) || (64 < c && c < 91) || (96 < c && c < 123))) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return the hostname for a InetAddress via AccessController.doPrivileged()
     *
     * @param InetAddress
     * @return the hostname for the given address
     */
    private String getHostName(final InetAddress address) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return address.getHostName();
            }
        });
    }

}
