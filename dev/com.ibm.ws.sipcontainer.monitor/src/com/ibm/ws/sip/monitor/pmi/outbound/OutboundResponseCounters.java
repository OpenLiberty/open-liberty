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
import com.ibm.ws.sip.monitor.mxbeans.OutboundResponseCountersMXBean;
import com.ibm.ws.sip.monitor.pmi.application.ApplicationModule;
import com.ibm.ws.sip.monitor.pmi.application.ApplicationsModule;

public class OutboundResponseCounters extends Meter implements
		OutboundResponseCountersMXBean {
	
	/** Singleton - initialized on activate */
    private static OutboundResponseCounters s_singleton = null;    
    
    
	@Override
	public long getTotalOutboundResponses(String appName, String code) {
		ApplicationsPMIListener appsModule = ApplicationsModule.getInstance();
		if (appsModule != null) {
			ApplicationModule module = (ApplicationModule) appsModule.getApplicationModule(appName);
			return module.getAppSessionsCounter().getOutboundResponseCount(Integer.parseInt(code));
		}
		return 0;
	}

	public static OutboundResponseCounters getInstance() {
		if (s_singleton == null)
			s_singleton = new OutboundResponseCounters();		
		return s_singleton;
	}


}
