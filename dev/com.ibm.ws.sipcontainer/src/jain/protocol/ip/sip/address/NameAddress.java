/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jain.protocol.ip.sip.address;

import jain.protocol.ip.sip.SipParseException;
import jain.protocol.ip.sip.header.NameAddressHeader;

import java.io.Serializable;

/**
 * This interface represents a user's display name and address.
 * NameAddress contains an optional display name for the user, which
 * can be displayed to an end-user, and the URI (most likely a SipURL) that
 * is the user's address.
 *
 * @see URI
 * @see SipURL
 * @see NameAddressHeader
 *
 * @version 1.0
 *
 */
public interface NameAddress extends Cloneable, Serializable
{
    
    /**
     * Gets boolean value to indicate if NameAddress
     * has display name
     * @return boolean value to indicate if NameAddress
     * has display name
     */
    public boolean hasDisplayName();
    
    /**
     * Removes display name from NameAddress (if it exists)
     */
    public void removeDisplayName();
    
    /**
     * Sets display name of Header
     * @param <var>displayName</var> display name
     * @throws IllegalArgumentException if displayName is null
     * @throws SipParseException if displayName is not accepted by implementation
     */
    public void setDisplayName(String displayName)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets display name of NameAddress
     * (Returns null id display name does not exist)
     * @return display name of NameAddress
     */
    public String getDisplayName();
    
    /**
     * Sets address of NameAddress
     * @param <var>address</var> address
     * @throws IllegalArgumentException if address is null or not from same
     * JAIN SIP implementation
     */
    public void setAddress(URI address)
                 throws IllegalArgumentException;
    
    /**
     * Gets string representation of NameAddress
     * @return string representation of NameAddress
     */
    public String toString();
    
    /**
     * Indicates whether some other Object is "equal to" this NameAddress
     * (Note that obj must have the same Class as this NameAddress - this means that it
     * must be from the same JAIN SIP implementation)
     * @param <var>obj</var> the Object with which to compare this NameAddress
     * @returns true if this NameAddress is "equal to" the obj
     * argument; false otherwise
     */
    public boolean equals(Object obj);
    
    /**
     * Creates and returns a copy of NameAddress
     * @returns a copy of NameAddress
     */
    public Object clone();
    
    /**
     * Gets address of NameAddress
     * @return address of NameAddress
     */
    public URI getAddress();
}
