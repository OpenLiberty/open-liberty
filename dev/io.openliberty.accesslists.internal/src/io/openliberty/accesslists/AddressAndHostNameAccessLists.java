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

import java.net.InetAddress;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.accesslists.filterlist.FilterList;
import io.openliberty.accesslists.filterlist.FilterListFastStr;
import io.openliberty.accesslists.filterlist.FilterListStr;

public class AddressAndHostNameAccessLists extends AddressAccessLists {

    public FilterListStr hostNameExcludeList = null;
    public FilterListStr hostNameIncludeList = null;
    protected boolean caseInsensitiveHostnames = true;
    public static final TraceComponent tc = Tr.register(AddressAndHostNameAccessLists.class,
            AccessListsConstants.TCP_TRACE_GROUP, AccessListsConstants.TCP_MESSAGES);

    public AddressAndHostNameAccessLists(FilterList _addressExcludeList, FilterList _addressIncludeList) {
        super(_addressExcludeList, _addressIncludeList);
    }

    /**
     * Constructor.
     * 
     * @param _addressExcludeList
     * @param _hostNameExcludeList
     * @param _includeAccess
     * @param _hostNameIncludeList
     * @param _caseInsensitiveHostnames
     */
    public AddressAndHostNameAccessLists(FilterList _addressExcludeList, FilterListStr _hostNameExcludeList,
            FilterList _includeAccess, FilterListStr _hostNameIncludeList, boolean _caseInsensitiveHostnames) {
        super(_addressExcludeList, _includeAccess);
        this.hostNameExcludeList = _hostNameExcludeList;
        this.hostNameIncludeList = _hostNameIncludeList;
        this.caseInsensitiveHostnames = _caseInsensitiveHostnames;
    }

    public AddressAndHostNameAccessLists(Lists l, boolean caseInsensitiveHostsNames) {
        this(l.addressExcludeList, l.hostNameExcludeList, l.addressIncludeList, l.hostNameIncludeList,
                caseInsensitiveHostsNames);
    }

    /**
     * Do we have include access lists, if so we must have the address on one of
     * them to allow access.
     * 
     * @return true if both address and hostname include lists are empty
     */
    protected boolean allIncludesEmpty() {
        return !addressIncludeList.getActive() && !hostNameIncludeList.getActive();
    }

    /**
     * Can we find the address in the hostname include list
     * 
     * @return true if found
     */
    protected boolean inHostNameIncludeList(InetAddress addr) {

        if (!hostNameIncludeList.getActive()) {
            return false;
        }

        String hostname = addr.getHostName();

        if (caseInsensitiveHostnames && (hostname != null)) {
            hostname = hostname.toLowerCase();
        }

        return hostNameIncludeList.findInList(hostname);

    }

    /**
     * Is this address' Host in the hostname excludes list
     * 
     * @param remoteAddr
     * 
     * @return true if excluded due to hostname
     */
    protected boolean inHostNameExcludeList(InetAddress remoteAddr) {

        if (!hostNameExcludeList.getActive()) {
            return false;
        }

        String hostname = remoteAddr.getHostName();
        if (caseInsensitiveHostnames && (hostname != null)) {
            hostname = hostname.toLowerCase();
        }

        if (hostNameExcludeList.findInList(hostname)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                Tr.event(tc, "Host name in exclude list, host name: " + remoteAddr.getHostName());
            return true;
        }

        return false;
    }

    /**
     * Can we find the address in one of the exlude lists
     * 
     * @param addr
     * @return true if excluded
     */
    protected boolean inExcludeList(InetAddress addr) {
        return inAddressExcludeList(addr) || inHostNameExcludeList(addr);
    }

    /**
     * We override the parent adding in hostname access lists
     * 
     * @param addr
     */
    @Override
    protected boolean nonEmptyIncludesAndNotFound(InetAddress addr) {

        if (allIncludesEmpty()) {
            return false;
        }

        if (inAddressIncludeList(addr) || inHostNameIncludeList(addr)) {
            return false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Address and host name not in include list, address: " + addr.getHostAddress() + " host name: "
                    + addr.getHostName());

        return true;
    }

    /**
     * Sets up all the fast search filter lists
     *
     * @param lists - a means of getting hold of the access lists
     * @return the compiled access checker
     */
    public static AddressAndHostNameAccessLists getInstance(AccessListKeysFacade lists) {
        Lists filterLists = new AddressAndHostNameAccessLists.Lists(lists);
        if (filterLists.active()) {
            return new AddressAndHostNameAccessLists(filterLists, lists.getCaseInsensitiveHostnames());
        } else {
            return null;
        }
    }

    /**
     * Helper to simplify some method signatures and share more code amongst clients
     */
    public static class Lists {
        public FilterList addressExcludeList;
        public FilterList addressIncludeList;
        public FilterListStr hostNameExcludeList;
        public FilterListStr hostNameIncludeList;

        public Lists(AccessListKeysFacade lists) {
            addressExcludeList = FilterList.create(lists.getAddressExcludeList());
            addressIncludeList = FilterList.create(lists.getAddressIncludeList());
            hostNameExcludeList = FilterListFastStr.create(lists.getHostNameExcludeList());
            hostNameIncludeList = FilterListFastStr.create(lists.getHostNameIncludeList());
        }

        @SuppressWarnings("unused")
        // No construction with null lists
        private Lists() {
        }

        /**
         * @return Does this access list need to be checked?
         */
        public boolean active() {
            return // @formatter:off
            addressExcludeList  != null &&  addressExcludeList.getActive() ||
            addressIncludeList  != null &&  addressIncludeList.getActive() ||
            hostNameExcludeList != null && hostNameExcludeList.getActive() ||
            hostNameIncludeList != null && hostNameIncludeList.getActive();
        }// @formatter:on
    }

}
