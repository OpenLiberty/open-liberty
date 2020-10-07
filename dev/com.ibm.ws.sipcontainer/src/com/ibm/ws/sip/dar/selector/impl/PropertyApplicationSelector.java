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
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.dar.selector.ApplicationSelector;
import com.ibm.ws.sip.dar.util.StateInfo;

/**
 * 
 * @author Roman Mandeleil
 */ 
public class PropertyApplicationSelector implements ApplicationSelector{
	
	private static final LogMgr c_logger = Log.get(PropertyApplicationSelector.class);
	private SipApplicationRouterInfo nullApplication = 
		new SipApplicationRouterInfo(null, null, null, null, null, null);

	private HashMap<String,	List<SipApplicationRouterInfo>> methodForApplicationMap = null;

	/**
	 * 
	 * @param methodForApplicationMap
	 */
	public PropertyApplicationSelector(HashMap<String, 
			List<SipApplicationRouterInfo>> methodForApplicationMap){
		this.methodForApplicationMap = methodForApplicationMap;
	}
	
	/** 
	 * Application composition is responsible for application execution order, 
	 * this method encapsualtes the logic of application selection in                 
	 * composition defined by property file.
	 */
	public SipApplicationRouterInfo getNextApplication(
			SipServletRequest initialRequest,
			SipApplicationRoutingRegion region, 
			SipApplicationRoutingDirective directive, 
			Serializable stateInfoS){
    	
		if (c_logger.isTraceEntryExitEnabled()){
            c_logger.traceEntry(this, "getNextApplication");
        }
		
		// NEW directive start the chain from the beggining
		if (directive.equals( SipApplicationRoutingDirective.NEW )){
			
			// No request recived 
			if (initialRequest == null) return null;

			StateInfo newStateInfo = new StateInfo();
			newStateInfo.setIndex(0);

			List<SipApplicationRouterInfo> appList = 
				methodForApplicationMap.get(initialRequest.getMethod());

			// There is no applications installed for 
			// that METHOD
			if (appList == null)     
				return nullApplication;
			
			if (0 >= appList.size()) 
				return nullApplication;
			
			SipApplicationRouterInfo nextApp = appList.get(0);
			
			
			String subscriberURI = getURIFromTheRequest(initialRequest, 
					nextApp.getSubscriberURI());
			
			SipApplicationRouterInfo returnVal = new SipApplicationRouterInfo(nextApp.getNextApplicationName(), 
					nextApp.getRoutingRegion(),
					subscriberURI, nextApp.getRoutes(),  
					nextApp.getRouteModifier(), newStateInfo);
			
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Default application router, property file strategy, next application has been selected: " + 
						nextApp.getNextApplicationName() + ".");
			}

    		if (c_logger.isTraceEntryExitEnabled()){
	            c_logger.traceExit(this, "getNextApplication");
	        }
			
			return returnVal;
		} 
		
		// CONTINUE directive, find the next application in the chain
		if (directive.equals( SipApplicationRoutingDirective.CONTINUE)){

			// No request or state recived 
			if (initialRequest == null) 
			{
	    		if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug("Application router insufficient state request is null");
	    		}

				throw new Error("Application router insufficient state request is null");
			}
			
			if (stateInfoS == null) 
			{
	    		if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug("Application router insufficient state statInfo is null");
	    		}

				throw new Error("Application router insufficient state statInfo is null");
			}

			StateInfo stateInfo = (StateInfo)stateInfoS;
			stateInfo.increaseLastIndex();
			
			List<SipApplicationRouterInfo> appList = 
				methodForApplicationMap.get(initialRequest.getMethod());
			
			// There is no applications installed for 
			// that METHOD or the chain is finished
			if (appList == null)     return this.nullApplication;
			if (stateInfo.getIndex() >= appList.size()) return this.nullApplication;

			SipApplicationRouterInfo nextApp = appList.get(stateInfo.getIndex());
			
			String subscriberURI = getURIFromTheRequest(initialRequest, 
					nextApp.getSubscriberURI());
			
			SipApplicationRouterInfo returnVal = new SipApplicationRouterInfo(nextApp.getNextApplicationName(),
					nextApp.getRoutingRegion(),
					subscriberURI, nextApp.getRoutes(),  
					nextApp.getRouteModifier(), stateInfo);
			
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug("Default application router, property file strategy, next application has been selected: " +
						nextApp.getNextApplicationName() +
						", chain index: " +
						((StateInfo)stateInfo).getIndex());
    		}

    		if (c_logger.isTraceEntryExitEnabled()){
	            c_logger.traceExit(this, "getNextApplication");
	        }

			return returnVal;
		}
		
		// The callee turned to be a caller so, we should start the 
		// chain in ORIGINATING region, the chain will be started from 
		// the current application (JSR 289 15.9.4)
		if (directive.equals( SipApplicationRoutingDirective.REVERSE)){
			
			StateInfo stateInfo = (StateInfo)stateInfoS;

			List<SipApplicationRouterInfo> appList = 
				methodForApplicationMap.get(initialRequest.getMethod());
			
			String currentApplicationName = 
				appList.get(stateInfo.getIndex()).getNextApplicationName();
			
			// Find the same application in ORIGINATING region
			int newState = 0;
			for (SipApplicationRouterInfo appInfo : appList){
				++newState;
				if (appInfo.getNextApplicationName().
						equals(currentApplicationName) && 
						appInfo.getRoutingRegion() == 
						  SipApplicationRoutingRegion.ORIGINATING_REGION ){
					
					String subscriberURI = getURIFromTheRequest(
							initialRequest, appInfo.getSubscriberURI());
					
					SipApplicationRouterInfo returnVal = new SipApplicationRouterInfo(
							appInfo.getNextApplicationName(), 
							appInfo.getRoutingRegion(), 
							subscriberURI, appInfo.getRoutes(), 
							appInfo.getRouteModifier(), newState);
					
					if (c_logger.isTraceEntryExitEnabled()){
			            c_logger.traceExit(this, "getNextApplication");
			        }
					
					return returnVal;
				}
			}
		}

		
		throw new Error("SipApplicationRoutingDirective is not one of NEW, CONTINUE, REVERSE");
		
	}
	
	/**
	 * The method returns URI from the request 
	 * according to header saved in the property file 
	 * @param request
	 * @param headerName
	 * @return
	 */
	private String getURIFromTheRequest(SipServletRequest request, 
			String headerName){
		
		URI returnVal =  null;
		if (headerName != null) {
			String upperCaseHeader = headerName.toUpperCase();
			
			if (upperCaseHeader.equals("DAR:TO")){
				returnVal = request.getTo().getURI();
			}  
			if (upperCaseHeader.equals("DAR:FROM")){
				returnVal = request.getFrom().getURI();
			}  
		}
		
		if (returnVal == null) return null;

		return returnVal.toString();
	}

	@Override
	public String toString() {
		
		StringBuffer objectString = new StringBuffer();
		
		Set<String> applications = 
			this.methodForApplicationMap.keySet();
		
		for (String app : applications){
			objectString.append(app).append(" = ").
				append(methodForApplicationMap.get(app)).
				append('\n');
		}
		
		return objectString.toString();
	}
	

}
