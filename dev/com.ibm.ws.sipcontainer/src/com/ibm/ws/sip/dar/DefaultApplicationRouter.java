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
package com.ibm.ws.sip.dar;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.ar.SipApplicationRouter;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipTargetedRequestInfo;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.sip.dar.repository.ApplicationRepository;
import com.ibm.ws.sip.dar.selector.ApplicationSelector;

/**
 * Default application router (JSR289)
 * application composition
 * @author Roman Mandeleil
 */
public class DefaultApplicationRouter implements SipApplicationRouter{
	
	private static final LogMgr c_logger = Log.get(DefaultApplicationRouter.class);
	private ApplicationRepository applicationRepository;
	
	

	public void applicationDeployed(List<String> newlyDeployedApplicationNames) {
		applicationRepository.applicationDeployed(newlyDeployedApplicationNames);
		
	}

	public void applicationUndeployed(List<String> undeployedApplicationNames) {
		applicationRepository.applicationUndeployed(undeployedApplicationNames);
	}

	public void destroy() {
		if(c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("destroy: ApplicationRouter finailized");
		}
	}

	 /**
	  * This method is called by the container when a servlet sends or proxies an 
	  * initial SipServletRequest. The application router returns a set of information. 
	  * See SipApplicationRouterInfo  for details.  
	  * @param initialRequest - request which asks for next application. 
	  * @param region         - region which will run the next application.
	  * @param directive      - directive which clarifies the. 
	  * @param targetedRequestInfo - If initialRequest is a targeted request, this object 
	  * 		gives the type of targeted request (ENCODED_URI, JOIN, REPLACES) and the 
	  * 		targeted application name. If the initialRequest is not targeted, this parameter 
	  * 		is null.	   
	  * @param stateInfo      - the state of the application chain.
	  * @return application selection result. If no applications are deployed this 
	  * 		method returns null.
	  *
	  *  @throws java.lang.NullPointerException - if the initialRequest is null 
	  *  @throws java.lang.IllegalStateException - if the application router has not been initialized yet 
	  */
	public SipApplicationRouterInfo getNextApplication(SipServletRequest initialRequest, 
			SipApplicationRoutingRegion region, 
			SipApplicationRoutingDirective directive, 
			SipTargetedRequestInfo targetedRequestInfo,
			Serializable stateInfo) {

		if( c_logger.isTraceEntryExitEnabled()){
			c_logger.traceEntry(null, "getNextApplication", 
					new Object[]{initialRequest, region, directive, stateInfo});
		}
		
		ApplicationSelector applicationSelector = 
			this.applicationRepository.getApplicationSelector();

		if( c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug(null, "getNextApplication", "applicationSelector="+applicationSelector);
		}
		
		SipApplicationRouterInfo sipApplicationRouterInfo = 
			applicationSelector.getNextApplication(initialRequest, region, 
					 				directive, stateInfo);
		
		if( c_logger.isTraceEntryExitEnabled()){
			c_logger.traceExit(null, "getNextApplication");
		}
		return sipApplicationRouterInfo;
	}

	/**
	 * init - method called when the ApplicationRouter is initialized.
	 */
	public void init() {
		
		this.applicationRepository = 
			ApplicationRepositoryFactory.createApplicationRepository();
		if( c_logger.isInfoEnabled()){
			c_logger.info("info.dar.init.2", Situation.SITUATION_CONFIGURE);
		}
	}

	/**
	 * init() -init the router with the list of strings is useless, 
	 * the applications will be loaded through property file
	 * or the WAS listener
	 */
	public void init(List<String> deployedApplicationNames) {
		init();
	}
	
	/**
	 * Empty method in this AR implelemtation
	 */
	public void init(Properties properties) throws IllegalStateException {
	}


	
}
