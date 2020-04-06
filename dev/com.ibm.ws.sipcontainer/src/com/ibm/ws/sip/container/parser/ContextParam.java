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
package com.ibm.ws.sip.container.parser;

/**
 * Holds the context-param from sip.xml
 * Used to pass this parameters to web.xml
 * 
 * @author yaronr
 */
public class ContextParam
{
    /**
     * Holds param-name
     */
    private String m_name = null;

    /**
	 * Holds param-value
	 */
    private String m_value = null;
   
   
    public ContextParam(String name, String value)
    {
    	m_name = name;
    	m_value  = value;	
    }
    
    /**
     * @return
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * @return
     */
    public String getValue()
    {
        return m_value;
    }

   

   

}
