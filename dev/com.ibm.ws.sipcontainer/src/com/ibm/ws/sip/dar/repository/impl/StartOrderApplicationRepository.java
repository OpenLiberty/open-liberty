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
package com.ibm.ws.sip.dar.repository.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipRouteModifier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.dar.ext.SipApplicationRouterInfoStrartOrder;
import com.ibm.ws.sip.dar.repository.ApplicationRepository;
import com.ibm.ws.sip.dar.selector.ApplicationSelector;
import com.ibm.ws.sip.dar.selector.impl.StartOrderApplicationSelector;

/**
 * 
 * This class receives update about each deployed / undeployed application
 * and stores each of them in active applications the list according to their start order.
 *  
 * @author anatf
 *
 */
public class StartOrderApplicationRepository implements ApplicationRepository{

	  /** Trace service */
    private static final TraceComponent tc = Tr.register(StartOrderApplicationRepository.class);
    
    /*
	 * Constants
	 */
	public static final SipApplicationRoutingRegion DEAFULT_ROUTING_REGION = SipApplicationRoutingRegion.NEUTRAL_REGION;
    public static final String SUBSCRIBER_URI = null;
    public static final SipRouteModifier DEAFULT_ROUTE_MODIFIER = SipRouteModifier.NO_ROUTE;

   
    /**
     * List of all started application
     */
    private List<SipApplicationRouterInfoStrartOrder> appInfoList = 
			new ArrayList<SipApplicationRouterInfoStrartOrder>();
    
    private StartOrderApplicationSelector applicationSelector;
    
    public StartOrderApplicationRepository() {
		this.applicationSelector = new StartOrderApplicationSelector(this.appInfoList);
	}
    
	@Override
	public void applicationDeployed(List<String> newlyDeployedApplicationNames) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationDeployed event. App name = " + newlyDeployedApplicationNames.toString());
		    		
		for (Iterator<String> itr = newlyDeployedApplicationNames.iterator(); itr.hasNext();){
			String appName = (String)itr.next();
			
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
	             Tr.debug(this, tc, "New Application Started: " + appName);
			 
			 SipApplicationRouterInfoStrartOrder sipApplicationRouterStartOrder = 
						new SipApplicationRouterInfoStrartOrder(appName);
			 
			 this.appInfoList.add(sipApplicationRouterStartOrder);
		   }
	}

	@Override
	public void applicationUndeployed(List<String> undeployedApplicationNames) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "applicationUndeployed event. App names = " + undeployedApplicationNames.toString());
		
		for (Iterator<String> itr = undeployedApplicationNames.iterator(); itr.hasNext();){
			String appName = (String)itr.next();
			SipApplicationRouterInfoStrartOrder app = null;
			// Find application to remove.
			for (SipApplicationRouterInfoStrartOrder currApp : this.appInfoList) {

				if (currApp.getNextApplicationName().equals(appName)) {
					app = currApp;
					if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
			             Tr.debug(this, tc, "Found application to remove: " + appName);

					break;
				}
			}
			this.appInfoList.remove(/*appName*/ app);
		}
	}

	
	@Override
	public ApplicationSelector getApplicationSelector() {
		return this.applicationSelector;
	}

}
