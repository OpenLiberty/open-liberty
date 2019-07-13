/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import com.ibm.ws.exception.ComponentDisabledException;
import com.ibm.ws.exception.RuntimeError;
import com.ibm.ws.exception.RuntimeWarning;
//TODO Liberty 
//import com.ibm.ws.sip.hamanagment.WSServiceEnablement;
//import com.ibm.ws.sip.hamanagment.drs.service.SIPDRSService;
//import com.ibm.ws.sip.hamanagment.ha.service.SIPHAComponentImpl;
//import com.ibm.ws.sip.hamanagment.ucf.service.SIPHAUCFServerComponentImpl;
//import com.ibm.ws.sip.hamanagment.ucf.slsp.service.SIPHAUCFSlspClientComponentImpl;
//import com.ibm.ws.sip.hamanagment.zos.SIPZOSManager;
//import com.ibm.wsspi.runtime.component.WsComponent;
//import com.ibm.wsspi.runtime.config.ConfigObject;

/**
 * Handles HA components. 
 * Use this launcher to initialize, start and stop HA components on demand. 
 * 
 * @author Nitzan Nissim
 */
public class SipSessionManagerLauncher {
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipSessionManagerLauncher.class);
    
    /**
     * Indicates if HA components started. Prevents stopping without starting.
     */
    private static boolean s_started = false;
    
    /**
     * HA components
     */
	/*TODO Liberty private static WsComponent[] _siphaComponents = new WsComponent[5];*/	
	
	/**
	 * initialize HA components
	 * @throws ComponentDisabledException
	 */
	public static void initialize() throws ComponentDisabledException{
//WDW        SIPContainer config = (SIPContainer)WebsphereCommonLauncher.getConfig();
        /*TODO Liberty ConfigObject config = WebsphereCommonLauncher.getConfig();
		if( c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug( null, "initialize", "Initializing HA components started");
	    }
	    
		String type = PropertiesStore.getInstance().getProperties().getString(HAProperties.REPLICATOR_TYPE);
		if (type != null && type.equalsIgnoreCase(HAProperties.REPLICATOR_TYPE_OBJECTGRID)
			|| type != null && type.equalsIgnoreCase(HAProperties.REPLICATOR_TYPE_JDBC)) 
		{
			WSServiceEnablement.getInstance().setSIPHAReplicationEnable(true);
			_siphaComponents[0] = new SIPZOSManager();
			_siphaComponents[1] = new SIPHAComponentImpl();
			_siphaComponents[2] = new SIPHAUCFServerComponentImpl();
			_siphaComponents[3] = new SIPHAUCFSlspClientComponentImpl();
		} else {
			_siphaComponents[0] = new SIPDRSService();			
			_siphaComponents[1] = new SIPZOSManager();
			_siphaComponents[2] = new SIPHAComponentImpl();
			_siphaComponents[3] = new SIPHAUCFServerComponentImpl();
			_siphaComponents[4] = new SIPHAUCFSlspClientComponentImpl();
		}
        
        
        for (int i = 0; i < 5; i++) {
            try {
                if (_siphaComponents[i] != null) 
                	_siphaComponents[i].initialize(config);
            } catch (ConfigurationWarning e) {
                throw new ComponentDisabledException( "HA component " + _siphaComponents[i] + " initialization failed.", e);
            } catch (ConfigurationError e) {
            	throw new ComponentDisabledException( "HA component " + _siphaComponents[i] + " initialization failed.", e);
            }
        }
        
        if( c_logger.isTraceDebugEnabled()){
            c_logger.traceDebug( null, "initialize", "Initializing HA components finished");
        }*/
	}
	/**
     * Override the start() method in ComponentImpl. 
     * Since Websphere has finished initialization, we can at this stage
     *  	get the	AppServerDispatcher
     */
    public static void start() throws RuntimeWarning, RuntimeError
    {
        /*TODO Liberty for (int i = 0; i < 5; i++) {
        	if (_siphaComponents[i] != null)
        		_siphaComponents[i].start();            
        }
        s_started = true;*/
    }
    
    /**
     * Override the stop() method in ComponentImpl. This is called when the server
     * is stopping.
     */
    public static void stop()
    {
    	/*TODO Liberty if(!s_started){
    		return;
    	}
    	
        for (int i = 4; i > -1; i--) {
        	if (_siphaComponents[i] != null)
        		_siphaComponents[i].stop();            
        }*/
    }
}
