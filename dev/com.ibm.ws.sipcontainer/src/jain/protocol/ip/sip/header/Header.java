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
package jain.protocol.ip.sip.header;

import jain.protocol.ip.sip.SipParseException;

import java.io.Serializable;

/**
 * This interface represents a generic SIP header.
 * A Header can be one of the following types:
 * <LI>General header  - can be in Request or Response</LI>
 * <LI>Request header  - can only be in Request</LI>
 * <LI>Response header - can only be in Response</LI>
 * <LI>Entity header   - describes Message body or resource</LI>
 * <p>
 * The order in which header fields with differing field names are
 * received is not significant. However, it is "good practice" to send
 * general-header fields first, followed by request-header or response-
 * header fields, and ending with the entity-header fields.
 * </p><p>
 * The order in which header fields with the same
 * field-name are received is significant, and thus a proxy MUST NOT
 * change the order of these field values when a message is forwarded.
 * </p><p>
 * Other Headers can be added as required; a server must ignore
 * Headers that it does not understand. A proxy must not remove or modify
 * Headers that it does not understand.
 * </p>
 *
 *
 * @version 1.0
 *
 */
public interface Header extends Cloneable, Serializable
{
    
    /**
     * Sets value of Header
     * @param <var>value</var> value
     * @throws IllegalArgumentException if value is null
     * @throws SipParseException if value is not accepted by implementation
     */
    public void setValue(String value)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets value of Header
     * @return value of Header
     */
    public String getValue();
    
    /**
     * Gets string representation of Header
     * @return string representation of Header
     */
    public String toString();
    
    /**
     * Indicates whether some other Object is "equal to" this Header
     * (Note that obj must be have same class as this Header - which means it
     * must be from same JAIN SIP implementation)
     * @param <var>obj</var> the Object with which to compare this Header
     * @returns true if this Header is "equal to" the obj
     * argument; false otherwise
     */
    public boolean equals(Object obj);
    
    /**
     * Creates and returns a copy of Header
     * @returns a copy of Header
     */
    public Object clone();
    
    /**
     * Gets name of Header
     * @return name of Header
     */
    public String getName();
}
