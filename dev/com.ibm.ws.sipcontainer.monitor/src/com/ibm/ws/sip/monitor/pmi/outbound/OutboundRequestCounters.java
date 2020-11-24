/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.monitor.pmi.outbound;

import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.ws.sip.container.pmi.listener.ApplicationsPMIListener;
import com.ibm.ws.sip.monitor.mxbeans.OutboundRequestCountersMXBean;
import com.ibm.ws.sip.monitor.pmi.application.ApplicationModule;
import com.ibm.ws.sip.monitor.pmi.application.ApplicationsModule;

public class OutboundRequestCounters extends Meter implements
		OutboundRequestCountersMXBean {
	
	/** Singleton - initialized on activate */
    private static OutboundRequestCounters s_singleton = null;    
    
    
	@Override
	public long getTotalOutboundRequests(String appName, String methodName) {
		ApplicationsPMIListener appsModule = ApplicationsModule.getInstance();
		if (appsModule != null) {
			ApplicationModule module = (ApplicationModule) appsModule.getApplicationModule(appName);
			return module.getAppSessionsCounter().getOutboundRequestCount(methodName);
		}
		return 0;
	}

	public static OutboundRequestCounters getInstance() {
		if (s_singleton == null)
			s_singleton = new OutboundRequestCounters();		
		return s_singleton;
	}


}
