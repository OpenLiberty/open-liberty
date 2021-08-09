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
package jain.protocol.ip.sip;

import jain.protocol.ip.sip.address.SipURL;
import jain.protocol.ip.sip.header.ParametersHeader;

import java.util.Iterator;

/**
 * This interface represents a list of parameters. It is a super-interface of
 * SipURL and ParametersHeader.
 *
 * @see SipURL
 * @see ParametersHeader
 *
 * @version 1.0
 */
public interface Parameters
{
    
    /**
     * Gets the value of specified parameter
     * (Note - zero-length String indicates flag parameter)
     * (Returns null if parameter does not exist)
     * @param <var>name</var> name of parameter to retrieve
     * @return the value of specified parameter
     * @throws IllegalArgumentException if name is null
     */
    public String getParameter(String name)
                   throws IllegalArgumentException;
    
    /**
     * Sets value of parameter
     * (Note - zero-length value String indicates flag parameter)
     * @param <var>name</var> name of parameter
     * @param <var>value</var> value of parameter
     * @throws IllegalArgumentException if name or value is null
     * @throws SipParseException if name or value is not accepted by implementation
     */
    public void setParameter(String name, String value)
                 throws IllegalArgumentException,SipParseException;
    
    /**
     * Gets boolean value to indicate if Parameters
     * has any parameters
     * @return boolean value to indicate if Parameters
     * has any parameters
     */
    public boolean hasParameters();
    
    /**
     * Gets boolean value to indicate if Parameters
     * has specified parameter
     * @return boolean value to indicate if Parameters
     * has specified parameter
     * @throws IllegalArgumentException if name is null
     */
    public boolean hasParameter(String name)
                    throws IllegalArgumentException;
    
    /**
     * Removes specified parameter from Parameters (if it exists)
     * @param <var>name</var> name of parameter
     * @throws IllegalArgumentException if parameter is null
     */
    public void removeParameter(String name)
                 throws IllegalArgumentException;
    
    /**
     * Removes all parameters from Parameters (if any exist)
     */
    public void removeParameters();
    
    /**
     * Gets Iterator of parameter names
     * (Note - objects returned by Iterator are Strings)
     * (Returns null if no parameters exist)
     * @return Iterator of parameter names
     */
    public Iterator getParameters();
}
