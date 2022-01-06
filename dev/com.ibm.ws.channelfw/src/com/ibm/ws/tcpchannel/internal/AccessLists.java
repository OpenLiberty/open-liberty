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
package com.ibm.ws.tcpchannel.internal;

import io.openliberty.accesslists.AccessListKeysFacade;
import io.openliberty.accesslists.AddressAndHostNameAccessLists;
import io.openliberty.accesslists.filterlist.FilterList;
import io.openliberty.accesslists.filterlist.FilterListStr;

/**
 * Wrapper class handling various include or exclude lists for accessing a TCP
 * channel inbound port. The generic access list logic is in the parent, this is
 * mostly ensuring the correct subtype is created to allow for TCP specific actions
 * to be added if desired.
 */
public class AccessLists extends AddressAndHostNameAccessLists {

    public AccessLists(FilterList _addressExcludeList, FilterListStr _hostNameExcludeList,
                       FilterList _addressIncludeList, FilterListStr _hostNameIncludeList, boolean _caseInsensitiveHostnames) {
        super(_addressExcludeList, _hostNameExcludeList, _addressIncludeList, _hostNameIncludeList, _caseInsensitiveHostnames);
    }

    /**
     * @param filterLists
     * @param caseInsensitiveHostnames
     */
    public AccessLists(Lists l, boolean caseInsensitiveHostnames) {
        this(l.addressExcludeList, l.hostNameExcludeList, l.addressIncludeList, l.hostNameIncludeList, caseInsensitiveHostnames);
    }

    /**
     * Sets up all the fast search filter lists
     *
     * @param lists - a means of getting hold of the access lists
     * @return the compiled access checker
     */
    public static AccessLists getInstance(AccessListKeysFacade lists) {
        Lists filterLists = new AddressAndHostNameAccessLists.Lists(lists);
        if (filterLists.active()) {
            return new AccessLists(filterLists, lists.getCaseInsensitiveHostnames());
        } else {
            return null;
        }
    }

}
