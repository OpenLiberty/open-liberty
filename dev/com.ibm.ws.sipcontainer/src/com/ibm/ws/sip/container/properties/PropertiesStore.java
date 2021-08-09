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
package com.ibm.ws.sip.container.properties;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.properties.CustPropSource;
import com.ibm.ws.sip.properties.SipPropertiesMap;
import com.ibm.ws.sip.properties.StackProperties;

/**
 * The one and only properties store of the container
 * 	holds all properties of the sip container and it's components
 * 
 * @author yaronr
 */
public class PropertiesStore
{
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(PropertiesStore.class);
	
	/**
     * Hold properties
     * This member is volatile for performing the optimized double-check locking for this singleton
     */
    protected volatile SipPropertiesMap m_properties = null;

    /**
     * Only one instance of the store
     */
    static private PropertiesStore c_instance = new PropertiesStore();

    /**
     * Return the singelton instance of properties store (create if needed)
     * @return the single instance of the PropertiesStore
     */
    static public PropertiesStore getInstance()
    {
        return c_instance;
    }

	/**
	 * Get all properties - loads default properties if they're not loaded yet 
	 * @return the properties of the sip container 
	 */
    public SipPropertiesMap getProperties()
    {
    	SipPropertiesMap result = m_properties;
    	// Have we been here?
        if (result == null)
        {
        	synchronized (this) {
        		result = m_properties;
        		if (result == null) {
		            // create the properties
        			m_properties = result = new SipPropertiesMap();
		
		            // load default properties
		            loadDefaultProperties(); 
        		}
        	}

            if (c_logger.isTraceDebugEnabled())
            {
                c_logger.traceDebug(this, "getProperties", 
                "Properties set by default values: " + m_properties.logProprs(CustPropSource.DEFAULT));
            }
        }
        return result;
    }
    
    /**
     * Load default properties and store them in m_properties
     * 	this is the first properties that are loaded
     * 	and some properties might be overridden later by
     * 	the WCCM configuration 
     */
    protected void loadDefaultProperties()
    {
    	if (c_logger.isTraceEntryExitEnabled()) {
 			c_logger.traceEntry(PropertiesStore.class.getName(),
 					"loadDefaultProperties");
 		}	
		CoreProperties.loadDefaultProperties(m_properties);
		StackProperties.loadDefaultProperties(m_properties);
    }
}
