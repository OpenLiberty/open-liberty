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
package io.openliberty.accesslists;

import java.net.Inet6Address;
import java.net.InetAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.accesslists.filterlist.FilterList;

/**
 * Wrapper class handling include or exclude lists of addresses for accessing an
 * inbound port.
 */
public class AddressAccessLists {

    public static final TraceComponent tc = Tr.register(AddressAccessLists.class, AccessListsConstants.TCP_TRACE_GROUP,
            AccessListsConstants.TCP_MESSAGES);

    public FilterList addressExcludeList = null;
    public FilterList addressIncludeList = null;

    public AddressAccessLists(FilterList _addressExcludeList, FilterList _addressIncludeList) {
        this.addressExcludeList = _addressExcludeList;
        this.addressIncludeList = _addressIncludeList;
    }

    /**
     * Query whether a given client address is denied by this configuration.
     * 
     * @param addr
     * @return Will return true if there is an access include list and the address
     *         is not on it or (lazy or) the address exclude list is not empty and
     *         the address is on it
     */
    public boolean accessDenied(InetAddress addr) {
        return nonEmptyIncludesAndNotFound(addr) || inExcludeList(addr);
    }

    /**
     * Are we denying due to the include list(s)?
     * 
     * Return true if the addressIncludeList is non-empty but the address is not in
     * it
     * 
     * @param addr
     * @return true if there is an include list present but the address is not in it
     */
    protected boolean nonEmptyIncludesAndNotFound(InetAddress addr) {

        if (!addressIncludeList.getActive()) {
            return false;
        }

        if (inAddressIncludeList(addr)) {
            return false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Address not in active include list, address: " + addr.getHostAddress());

        return true;
    }

    /**
     * Returns true if the address is on the include list
     * 
     * @param addr
     * @return address found
     */
    protected boolean inAddressIncludeList(InetAddress addr) {

        if (!addressIncludeList.getActive()) {
            return false;
        }

        if (addr instanceof Inet6Address) {
            return addressIncludeList.findInList6(addr.getAddress());
        } else {
            return addressIncludeList.findInList(addr.getAddress());
        }
    }

    /**
     * Are we denying access due to the excludeList(s)?
     * 
     * @param addr
     * @return true if the address in on a/the excludes list
     */
    protected boolean inExcludeList(InetAddress addr) {
        // At this level we only handle ip addresses
        return inAddressExcludeList(addr);
    }

    /**
     * Return true if there is an addressExcludeList and the parameter address is on
     * it
     * 
     * @param addr
     * @return true if the address if found on an exclude list
     */
    protected boolean inAddressExcludeList(InetAddress addr) {

        if (!addressExcludeList.getActive()) {
            return false;
        }

        if (addr instanceof Inet6Address) {
            if (addressExcludeList.findInList6(addr.getAddress())) {
                // close the excluded socket connection
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Address (IPv6) in exclude list, address: " + addr.getHostAddress());
                return true;
            }
        } else {
            if (addressExcludeList.findInList(addr.getAddress())) {
                // close the excluded socket connection
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Address in exclude list, address: " + addr.getHostAddress());
                return true;
            }
        }

        return false;
    }
}
