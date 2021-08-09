/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.servlets;

import jain.protocol.ip.sip.Parameters;
import jain.protocol.ip.sip.SipParseException;

import java.util.HashMap;
import java.util.Iterator;

/**
 * @author Amir Perlman, 31/03/2003
 *
 * Implementation for the Parameter API defined by Jain. 
 */
public class ParametersImpl implements Parameters, Cloneable
{
    private HashMap m_params = new HashMap(10);

    /**
     * @see jain.protocol.ip.sip.Parameters#getParameter(java.lang.String)
     */
    public String getParameter(String name)
    {
        return (String) m_params.get(name);
    }

    /**
     * @see jain.protocol.ip.sip.Parameters#getParameters()
     */
    public Iterator getParameters()
    {
        return m_params.keySet().iterator();
    }

    /**
     * @see jain.protocol.ip.sip.Parameters#hasParameter(java.lang.String)
     */
    public boolean hasParameter(String name)
    {
        return m_params.containsKey(name);
    }

    /**
     * @see jain.protocol.ip.sip.Parameters#hasParameters()
     */
    public boolean hasParameters()
    {
        return !m_params.isEmpty();
    }

    /**
     * @see jain.protocol.ip.sip.Parameters#removeParameter(java.lang.String)
     */
    public void removeParameter(String name)
    {
        m_params.remove(name);

    }

    /**
     * @see jain.protocol.ip.sip.Parameters#removeParameters()
     */
    public void removeParameters()
    {
        m_params.clear();
    }

    /**
     * @see jain.protocol.ip.sip.Parameters#setParameter(java.lang.String, java.lang.String)
     */
    public void setParameter(String name, String value)
        throws IllegalArgumentException, SipParseException
    {
        m_params.put(name, value);

    }

    /** 
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException
    {
        ParametersImpl cloned = (ParametersImpl) super.clone();
        cloned.m_params = (HashMap) m_params.clone();

        return cloned;
    }

}
