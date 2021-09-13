/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

import java.net.Inet6Address;
import java.net.InetAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Wrapper class handling various include or exclude lists for accessing
 * a TCP channel inbound port.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.AccessLists}
 */
public class AccessLists {
    private FilterList excludeAccess = null;
    private FilterList includeAccess = null;
    private FilterListStr excludeAccessNames = null;
    private FilterListStr includeAccessNames = null;

    private boolean caseInsensitiveHostnames = true; // F184719

    private static final TraceComponent tc = Tr.register(AccessLists.class, TCPMessageConstants.NETTY_TRACE_NAME, TCPMessageConstants.TCP_BUNDLE);

    /**
     * Constructor.
     * 
     * @param _excludeAccess
     * @param _excludeAccessNames
     * @param _includeAccess
     * @param _includeAccessNames
     * @param _caseInsensitiveHostnames
     */
    public AccessLists(FilterList _excludeAccess, FilterListStr _excludeAccessNames, FilterList _includeAccess,
                       FilterListStr _includeAccessNames, boolean _caseInsensitiveHostnames) {
        this.excludeAccess = _excludeAccess;
        this.includeAccess = _includeAccess;
        this.excludeAccessNames = _excludeAccessNames;
        this.includeAccessNames = _includeAccessNames;
        this.caseInsensitiveHostnames = _caseInsensitiveHostnames;
    }

    protected static AccessLists getInstance(TCPConfigurationImpl config) {
        AccessLists retVal = null;
        boolean haveAList = false;
        FilterList excludeAccess = null;
        FilterList includeAccess = null;
        FilterListStr excludeAccessNames = null;
        FilterListStr includeAccessNames = null;

        String[] sExclude = config.getAddressExcludeList();
        excludeAccess = new FilterList();
        if (sExclude != null) {
            excludeAccess.buildData(sExclude, false);
            excludeAccess.setActive(true);
            haveAList = true;
        }

        sExclude = config.getHostNameExcludeList();
        excludeAccessNames = new FilterListFastStr();
        if (sExclude != null) {
            if (excludeAccessNames.buildData(sExclude) == false) {
                excludeAccessNames = new FilterListSlowStr();
                excludeAccessNames.buildData(sExclude);
            }
            excludeAccessNames.setActive(true);
            haveAList = true;
        }

        String[] sInclude = config.getAddressIncludeList();
        includeAccess = new FilterList();
        if (sInclude != null) {
            includeAccess.buildData(sInclude, false);
            includeAccess.setActive(true);
            haveAList = true;
        }

        sInclude = config.getHostNameIncludeList();
        includeAccessNames = new FilterListFastStr();
        if (sInclude != null) {
            if (includeAccessNames.buildData(sInclude) == false) {
                includeAccessNames = new FilterListSlowStr();
                includeAccessNames.buildData(sInclude);

            }
            includeAccessNames.setActive(true);
            haveAList = true;
        }

        if (haveAList) {
            retVal = new AccessLists(excludeAccess, excludeAccessNames, includeAccess, includeAccessNames,
                            config.getCaseInsensitiveHostnames());
        }

        return retVal;
    }

    /**
     * Query whether a given client address is denied by this configuration.
     * 
     * @param remoteAddr
     * @return boolean
     */
    public boolean accessDenied(InetAddress remoteAddr) {

        String hostname = null; //F184719

        // check the inclusion lists first to see if the client matches
        if (includeAccess.getActive() || includeAccessNames.getActive()) {
            boolean closeSocket = true;

            if (includeAccess.getActive()) {
                if (remoteAddr instanceof Inet6Address) {
                    if (includeAccess.findInList6(remoteAddr.getAddress())) {
                        closeSocket = false;
                    }
                } else {
                    if (includeAccess.findInList(remoteAddr.getAddress())) {
                        closeSocket = false;
                    }
                }
            }

            if (closeSocket && includeAccessNames.getActive()) {
                // look at hostnames to check inclusion

                hostname = remoteAddr.getHostName();
                if (caseInsensitiveHostnames && (hostname != null)) {
                    hostname = hostname.toLowerCase();
                }

                if (includeAccessNames.findInList(hostname)) {
                    closeSocket = false;
                }
            }

            if (closeSocket) {
                // close the excluded socket connection
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Address and host name not in include list, address: " + remoteAddr.getHostAddress() + " host name: " + remoteAddr.getHostName());
                return true;
            }
        }

        if (excludeAccess.getActive() || excludeAccessNames.getActive()) {
            boolean closeSocket = false;

            if (excludeAccess.getActive()) {
                if (remoteAddr instanceof Inet6Address) {
                    if (excludeAccess.findInList6(remoteAddr.getAddress())) {
                        // close the excluded socket connection
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(tc, "Address (IPv6) in exclude list, address: " + remoteAddr.getHostAddress());
                        return true;
                    }
                } else {
                    if (excludeAccess.findInList(remoteAddr.getAddress())) {
                        // close the excluded socket connection
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(tc, "Address in exclude list, address: " + remoteAddr.getHostAddress());
                        return true;
                    }
                }
            }

            if (closeSocket == false && excludeAccessNames.getActive()) {
                // look at hostnames to check exclusion

                hostname = remoteAddr.getHostName();
                if (caseInsensitiveHostnames && (hostname != null)) {
                    hostname = hostname.toLowerCase();
                }
                if (excludeAccessNames.findInList(hostname)) {
                    // close the excluded socket connection
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        Tr.event(tc, "Host name in exclude list, host name: " + remoteAddr.getHostName());
                    return true;
                }
            }
        } // end if(there are excludes to check)

        return false;
    }

}
