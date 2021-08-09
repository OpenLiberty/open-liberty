/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.servlet;

import java.io.IOException;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.RealTimeDaemon;

public class ESIProcessorKeepAliveDaemon extends RealTimeDaemon{

	ESIProcessor _esiProc = null;
	
	private static final TraceComponent tc = 
		Tr.register(ESIProcessorKeepAliveDaemon.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
	
	protected ESIProcessorKeepAliveDaemon(long timeInterval) { //interval in milliseconds
		super(timeInterval);
		if (tc.isDebugEnabled()){
			Tr.debug(tc, "Spawned ESIProcessorKeepAliveDaemon with a frequency of "+ timeInterval+" ms");
		}
	}
	
	void setESIProcessor (ESIProcessor esiP){
		_esiProc = esiP;
	}
	
	/**
	 * Send a keep alive message i.e. getPID message to the plugin at 
	 * the specified timeinterval to keep the connection alive.
	 */
	@Override
	protected void wakeUp(long startDaemonTime, long startWakeUpTime) {
		
		if (tc.isDebugEnabled()){
			Tr.debug(tc, "sending a keep alive initPID to the plugin for "+_esiProc);
		}
		
		try {
			if (null != _esiProc){
				_esiProc.initPID();
			}			
		} catch (IOException e) {			
			Tr.debug(tc, "Error encountered when writing a keep alive initPID message to the plugin for "+_esiProc);
			_esiProc = null;
		}		
	}
	
	public void stop(){		
		if (tc.isDebugEnabled()){
			Tr.debug(tc, "stop called on the ESIProcessorKeepAliveDaemon");
		}
		super.stop();
	}
	
}
