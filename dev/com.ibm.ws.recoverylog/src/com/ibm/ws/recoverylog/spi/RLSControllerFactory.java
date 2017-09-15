/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: RLSControllerFactory
//------------------------------------------------------------------------------
class RLSControllerFactory {

	 /**
     * WebSphere RAS TraceComponent registration
     */
    private static final TraceComponent tc = Tr.register(RLSControllerFactory.class, TraceConstants.TRACE_GROUP, TraceConstants.NLS_FILE);
    
	/**
	 * Single instance of the RLSController
	 */
	private static RLSController _instance;
	
	/**
     * Constructor declared private to prevent public instance creation
     */
	private RLSControllerFactory()
	{
		 if (tc.isEntryEnabled()) Tr.entry(tc, "RLSControllerFactory");
    	 if (tc.isEntryEnabled()) Tr.exit(tc, "RLSControllerFactory", this);
	}
	
	/**
     * Returns the singleton RLSController to which calls to suspend and resume are
     * delegated.
     * 
     */
	static RLSController getRLSController() throws Exception
	{
		if (tc.isEntryEnabled()) Tr.entry(tc, "getRLSController");
		
		if (_instance == null)      
	    {
	        try
	        {
	          _instance = (RLSController)Class.forName("com.ibm.ws.recoverylog.spi.RLSControllerImpl").newInstance();
	        }
	        catch (UnsupportedOperationException uoe)
	        {
	            // No FFDC code needed.
	            // Catch and swallow this exception
	        }
	        catch (Exception e)
	        {
	          FFDCFilter.processException(e, "com.ibm.ws.recoverylog.spi.RLSControllerFactory", "75");
	          if (tc.isEventEnabled()) Tr.event(tc, "Rethrowing exception");
	          if (tc.isEntryEnabled()) Tr.exit(tc, "getRLSController", e);
	          throw e;
	        }
	    }
		
		if (tc.isEntryEnabled()) Tr.exit(tc, "getRLSController", _instance);
		
	    return _instance;  
	}
}
