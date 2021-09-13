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

/**
 * 
 * Interface for building an access list and for determining if an
 * address is in an access list.
 * 
 * Taken from {@link com.ibm.ws.tcpchannel.internal.FilterListStr}
 */
public interface FilterListStr {

    /**
     * Build the address tree from a string array which contains valid
     * URL addresses.
     * 
     * @param data
     *            list of URL address which are
     *            to be used to create a new address tree. An address may start with
     *            a wildcard (for example: "*.Rest.Of.Address"),
     *            otherwise wildcards may not be used.
     * @return boolean
     */
    boolean buildData(String[] data);

    /**
     * Determine if an address is in the address tree
     * 
     * @param address
     *            address to look for
     * @return true if this address is found in the address tree, false if
     *         it is not.
     */
    boolean findInList(String address);

    /**
     * Sets if the address list is now active or dormant
     * 
     * @param value
     *            true if address list is to be active, else false
     */
    void setActive(boolean value);

    /**
     * Gets if the address list is now active or dormant
     * 
     * @return boolean
     */
    boolean getActive();

}
