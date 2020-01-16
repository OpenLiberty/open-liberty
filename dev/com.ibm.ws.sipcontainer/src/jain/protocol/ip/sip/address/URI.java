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

import java.io.Serializable;

/**
 * This interface represents a Universal Resource Identifier.
 * A URI contains a scheme and the scheme data which, in combination,
 * provide a means to locate a resource. This API defines one such scheme
 * - "sip", and has a SipURL class to represent this type of URI.
 *
 * @see SipURL
 *
 * @version 1.0
 *
 */
public interface URI extends Cloneable, Serializable
{
    
    /**
     * Gets string representation of URI
     * @return string representation of URI
     */
    public String toString();
    
    /**
     * Gets scheme data of URI
     * @return scheme data of URI
     */
    public String getSchemeData();
    
    /**
     * Sets scheme of URI
     * @param <var>scheme</var> scheme
     * @throws IllegalArgumentException if scheme is null
     * @throws SipParseException if scheme is not accepted by implementation
     */
    public void setScheme(String scheme)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets scheme of URI
     * @return scheme of URI
     */
    public String getScheme();
    
    /**
     * Indicates whether some other Object is "equal to" this URI
     * (Note that obj must have the same Class as this URI - this means that it
     * must be from the same JAIN SIP implementation)
     * @param <var>obj</var> the Object with which to compare this URI
     * @returns true if this URI is "equal to" the obj
     * argument; false otherwise (equality of URI's is defined in RFC 2068) 
     */
    public boolean equals(Object obj);
    
    /**
     * Creates and returns a copy of URI
     * @returns a copy of URI
     */
    public Object clone();
    
    /**
     * Sets scheme data of URI
     * @param <var>schemeData</var> scheme data
     * @throws IllegalArgumentException if schemeData is null
     * @throws SipParseException if schemeData is not accepted by implementation
     */
    public void setSchemeData(String schemeData)
                 throws IllegalArgumentException,SipParseException;
}
