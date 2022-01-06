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
package com.ibm.ws.udpchannel.internal;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.accesslists.AccessListKeysFacade;
import io.openliberty.accesslists.AddressAccessLists;
import io.openliberty.accesslists.filterlist.FilterList;

/**
 * This class allows for UDP type specific function to be added if needed.
 *
 */
public class AccessLists extends AddressAccessLists {

    static final TraceComponent tc = Tr.register(AccessLists.class, UDPMessages.TR_GROUP, UDPMessages.TR_MSGS);

    /**
     * Constructor.
     *
     * @param _addressExcludeList
     * @param _addressIncludeList
     */
    public AccessLists(FilterList _addressExcludeList, FilterList _addressIncludeList) {
        super(_addressExcludeList, _addressIncludeList);
    }

    /**
     * @param keys the means to get the address access lists
     * @return the created AccessList or null
     */
    protected static AccessLists getInstance(AccessListKeysFacade keys) {

        FilterList addressExcludeList = FilterList.create(keys.getAddressExcludeList());
        FilterList addressIncludeList = FilterList.create(keys.getAddressIncludeList());

        if (addressExcludeList.getActive() || addressIncludeList.getActive()) {
            return new AccessLists(addressExcludeList, addressIncludeList);
        } else {
            return null;
        }
    }

}
