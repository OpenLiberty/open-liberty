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
package com.ibm.ws.sip.dar.selector.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.ws.sip.dar.ext.SipApplicationRouterInfoStrartOrder;
import com.ibm.ws.sip.dar.selector.ApplicationSelector;
import com.ibm.ws.sip.dar.util.StateInfo;

/**
 * The class encapsulates selection logic
 * according application start order strategy 
 * 
 * @author anatf
 *
 */
public class StartOrderApplicationSelector implements ApplicationSelector{
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(StartOrderApplicationSelector.class);


    private SipApplicationRouterInfo nullApplication = 
    		new SipApplicationRouterInfo(null, null, null, null, null, null);
    	
    private List<SipApplicationRouterInfoStrartOrder> appInfoList = 
    		new ArrayList<SipApplicationRouterInfoStrartOrder>();
    
    
    /**
	 * Ctor
	 * @param appInfoList
	 */
	public StartOrderApplicationSelector(List<SipApplicationRouterInfoStrartOrder> appInfoList){
		this.appInfoList = appInfoList;
	}
	
	/**
	 * Return next application.
	 */
	public SipApplicationRouterInfo getNextApplication(
			SipServletRequest initialRequest,
			SipApplicationRoutingRegion region,
			SipApplicationRoutingDirective directive, Serializable stateInfo) {

		if (c_logger.isTraceDebugEnabled()){
            c_logger.traceDebug("getNextApplication: ");
		}
		
		
		SipApplicationRouterInfo nextApplicationForMatch = null;
		
		// Find next application for JSR116 application
		if (((SipServletRequestImpl)initialRequest).isAppInvoked116Type()){
			nextApplicationForMatch = findApplicationAccordingJSR116((SipServletRequestImpl)initialRequest);
		} else {
			// Find next application for JSR289 application
			if (directive.equals( SipApplicationRoutingDirective.NEW )){
				nextApplicationForMatch = performNewDirective(stateInfo);
			} else {
				nextApplicationForMatch = performContinueDirective(stateInfo);
			}
		}
		
		if (c_logger.isTraceDebugEnabled()){
			
			String application = null;
			
			if (nextApplicationForMatch != null){
				application = nextApplicationForMatch.getNextApplicationName();
			}
			c_logger.traceDebug("Next application to try siplet match - " + application);
        }

		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("getNextApplication");
        }
		
		return nextApplicationForMatch;
	}
	
	/**
	 * Continue composition chain
	 * @param initialRequest
	 * @return
	 */
	private SipApplicationRouterInfo performContinueDirective(
			Serializable stateInfo){
		if (stateInfo == null) {
			if (c_logger.isErrorEnabled())
				c_logger.error("error.dar.selector.1");
			return null;
		}
		
		((StateInfo)stateInfo).increaseLastIndex();
		int lastIndex = ((StateInfo)stateInfo).getIndex();
		
		
		// Check end of application chain 
		if (lastIndex >= appInfoList.size()) 
			return nullApplication;
		
		SipApplicationRouterInfoStrartOrder nextApp = appInfoList.get(lastIndex);
		((StateInfo)stateInfo).setIndex(lastIndex);
		
		SipApplicationRouterInfoStrartOrder returnVal = new SipApplicationRouterInfoStrartOrder(nextApp.getNextApplicationName(), 
				null, nextApp.getRoutes(), nextApp.getRoutingRegion(), 
				nextApp.getRouteModifier(), stateInfo);
		
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("CWSCT0408I: Default application router, weight strategy, next application has been selected: " + nextApp.getNextApplicationName() 
					+ ", chain index: " + ((StateInfo)stateInfo).getIndex() + ". ");
        }
		
		return returnVal;
	}
	
	
	/**
	 * JSR 116 doesn't specify any routing directives, 
	 * the application process should allways be as if CONTINUE 
	 * directive was selected
	 * @param initialRequest
	 * @return
	 */
	private SipApplicationRouterInfo findApplicationAccordingJSR116(SipServletRequestImpl initialRequest){
	
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("findApplicationAccordingJSR116");
        }
		
		// In JSR 116 the request can be created from another request
		// in that case the state is set and application selection process can
		// be proceed as JSR 289
		StateInfo stateInfo = null;

		if (initialRequest.getStateInfo() != null) {
			stateInfo = (StateInfo)initialRequest.getStateInfo();
		}

		else {
			initialRequest
					.setDirective(SipApplicationRoutingDirective.CONTINUE);
			String appInvokedName = initialRequest.getAppInvokedName();

			//check if this is async request, if so we can skip the routing procedure
			if (SipServletsFactoryImpl.UNKNOWN_APPLICATION.equals(appInvokedName)){
				
				if (c_logger.isTraceDebugEnabled()){
					c_logger.traceDebug("unknown application is used, probably async request,  the routing procedure will be skipped");
		        }
				
				return null;
			}
			
			int appIndex = 0;

			// Find index of application which created the request,
			for (SipApplicationRouterInfoStrartOrder currApp : appInfoList) {
				if (currApp.getNextApplicationName().equals(appInvokedName)) {
					stateInfo = new StateInfo();
					stateInfo.setIndex(appIndex);
					initialRequest.setStateInfo(stateInfo);
				}
				++appIndex;
			}
		}

		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("findApplicationAccordingJSR116");
        }
		return performContinueDirective(stateInfo);
	}
	

	/**
	 * Start the chain from the beginning
	 * @param initialRequest
	 * @return
	 */
	private SipApplicationRouterInfo performNewDirective(
			Serializable stateInfo){
		
		StateInfo newStateInfo = new StateInfo();
		newStateInfo.setIndex(0);

		if (0 >= appInfoList.size()) 
			return nullApplication;
		
		SipApplicationRouterInfoStrartOrder nextApp = appInfoList.get(0);
		
		SipApplicationRouterInfoStrartOrder returnVal = new SipApplicationRouterInfoStrartOrder(nextApp.getNextApplicationName(), 
				null, nextApp.getRoutes(), nextApp.getRoutingRegion(), 
				nextApp.getRouteModifier(), newStateInfo);

		if (c_logger.isTraceDebugEnabled()){
			
			String application = null;
			
			if (nextApp != null){
				application = nextApp.getNextApplicationName(); 
			}
			
			c_logger.traceDebug("Default application router, start order strategy, next application has been selected: " +  
					application + ". ");
        }
		
		return returnVal;
	}
	
	@Override
	public String toString() {
		
		StringBuffer objectString = new StringBuffer();
		
		for (SipApplicationRouterInfoStrartOrder app : appInfoList){
			objectString.append(app).append("\n");
		}
		
		return objectString.toString();
	}
}
