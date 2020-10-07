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

/**
 * This interface represents the Allow entity-header.
 * The AllowHeader specifies a method supported by
 * the resource identified by the Request-URI of a Request.
 * An AllowHeader must be present in a Response with a status code
 * METHOD_NOT_ALLOWED, and should be present in a
 * Response to an OPTIONS Request
 *
 * @version 1.0
 *
 */
public interface AllowHeader extends Header
{
    
    /**
     * Sets method of AllowHeader
     * @param <var>method</var> method
     * @throws IllegalArgumentException if method is null
     * @throws SipParseException if method is not accepted by implementation
     */
    public void setMethod(String method)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets method of AllowHeader
     * @return method of AllowHeader
     */
    public String getMethod();
    
    //////////////////////////////////////////////////////////////
    
    /**
     * Name of AllowHeader
     */
    public final static String name = "Allow";
}
