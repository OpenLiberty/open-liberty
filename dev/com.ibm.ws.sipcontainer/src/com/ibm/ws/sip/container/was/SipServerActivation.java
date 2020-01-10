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
package com.ibm.ws.sip.container.was;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
//TODO Liberty:
//import com.ibm.wsspi.runtime.provisioning.ServerActivation;
//import com.ibm.wsspi.runtime.provisioning.ServerActivationHelper;
//import com.ibm.websphere.models.config.serverindex.ServerTypeConstants;
//import com.ibm.wsspi.runtime.provisioning.ComponentInfoFactory;

/**
 * @author anat
 * 
 * SipServerActivation clss implements com.ibm.wsspi.provisioning.ServerActivation 
 * interface. During the server startup, provisioning calls the getActivationPlan() 
 * method for both classes. SipServerActivation class checks if it is running on 
 * server type "Application Server". 
 * If true - ComponentInfo contains SIPContainer component retuned 
 * (otherwise null is returned).
 * 
 */
public class SipServerActivation /*TODO Liberty implements ServerActivation*/ {
	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(SipServerActivation.class);

	/*TODO Liberty private static final String APPLICARION_SERVER = ServerTypeConstants.APPLICATION_SERVER;*/
	private static final String CONTAINER_SERVICE = "WS_SipContainer";
	
	
	/**
	 * @see com.ibm.wsspi.runtime.provisioning.ServerActivation#getActivationPlan()
	 */
	/*TODO Liberty public List getActivationPlan() throws RuntimeError {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getActivationPlan", "Starting to run SipServerActivation");
		}
		String serverType;
		
		try { 	
    	    serverType = ServerActivationHelper.getServerType();
    	} 
		catch(RuntimeError e) {
	        
    		if(c_logger.isErrorEnabled())
			{
    			Object[] args = { e.getMessage()}; 
				c_logger.error("error.sip.get.server.type", Situation.SITUATION_START, args);
			}   
	    	
    		throw e;
    	}
		
//		 Generate the list of proxy components needed for this configuration
    	List componentList = null; 
    	
		if(serverType.equals(APPLICARION_SERVER)){
			// SipConainer should run only in ApplicationServer.
			componentList = new LinkedList();
			componentList.add(ComponentInfoFactory.createComponentInfo(CONTAINER_SERVICE));
		}
		else{
			if(c_logger.isErrorEnabled())
			{
    			Object[] args = { serverType}; 
    			c_logger.error("error.sip.cannot.run.on.server", Situation.SITUATION_START, args);
			}  
		}
		
    	return componentList;
	}*/
}
