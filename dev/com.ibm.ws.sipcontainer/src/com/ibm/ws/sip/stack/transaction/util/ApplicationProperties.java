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
package com.ibm.ws.sip.stack.transaction.util;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.properties.StackProperties;

/**
 * Holds properties for the application
 * @author amirk
 */
public class ApplicationProperties 
{	
	/**
	 * Class Logger. 
	 */
	private static final transient LogMgr c_logger = Log
			.get(ApplicationProperties.class);
	
	/** properties Object */
	private static SipPropertiesMap m_properties = null; 

	private ApplicationProperties()
	{			
	}
	
	public static  SipPropertiesMap getProperties()
	{
		if( m_properties == null )
		{
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(ApplicationProperties.class, "getProperties", 
				"Warning, trying to read properties, before they were loaded. Loading default values");
			}
			m_properties = new SipPropertiesMap();
			StackProperties.loadDefaultProperties(m_properties);
		}
		return m_properties;
	}
	
	public static void setProperties( SipPropertiesMap prop )
	{
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(ApplicationProperties.class.getName(), "setProperties");
		}
		
		m_properties = prop;
	}
}
