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
package com.ibm.ws.sip.container.router;

import jain.protocol.ip.sip.header.RouteHeader;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.URI;
import javax.servlet.sip.ar.SipApplicationRouter;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipRouteModifier;
import javax.servlet.sip.ar.SipTargetedRequestInfo;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.container.matching.SipServletsMatcher;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.servlets.IncomingSipServletRequest;
import com.ibm.ws.sip.container.servlets.SipServletRequestImpl;
import com.ibm.ws.sip.container.servlets.SipServletsFactoryImpl;
import com.ibm.ws.sip.container.servlets.SipURIImpl;
import com.ibm.ws.sip.container.util.SipUtil;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.webcontainer.webapp.WebApp;

/**
 * ApplicationPathSelector - wraps all the composition logic, 
 * which contains application selection by application router, 
 * and sipplet selection by SipServletsMatcher 
 *  
 * @author Roman Mandeleil 
 */
public class ApplicationPathSelector {

	
	/*
	 * Trace variable
	 */
	private static final TraceComponent tc = Tr
			.register(ApplicationPathSelector.class);
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(ApplicationPathSelector.class);

	private SipServletsMatcher sipletMatcher = null;
	private SipApplicationRouter applicationRouter = null;
	
	
	private SipAppDescManager sipAppDescManager = null;
	

	public ApplicationPathSelector(SipServletsMatcher sipletMatcher,
			SipApplicationRouter applicationRouter) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "Ctor",
					"sipletMatcher= " + sipletMatcher + ", applicationRouter="+applicationRouter);
		}
		this.sipletMatcher = sipletMatcher;
		this.applicationRouter = applicationRouter;
		this.sipAppDescManager = SipAppDescManager.getInstance();
	}

	 /**
     * Returns all active applications
     * @return
     */
    public LinkedList<SipAppDesc> getAllApps() {
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "getAllApps");
		}
		return sipletMatcher.getAllApps();
				
	}
	/**
	 * 1) Application selection by application router
	 * 2) Sipplet selection by SipServletMatcher
	 * @param request - request to find application for 
	 * @return - sipplet to invoke, null in case of chain end
	 */
	public SipServletDesc findSippletMatch(SipServletRequestImpl request, SipTargetedRequestInfo sipTargetedRequestInfo) {

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "findSippletMatch");
		}
		
		// The application has been selected already before 
		// the message was sent in loopback
		if (request.getNextApplication() != null) {
			SipServletDesc matchedSiplet = null;
			matchedSiplet = sipletMatcher.matchSipletForApplication(request, request
					.getNextApplication());
			
			if (matchedSiplet != null) {
				SipAppDesc appDesc = matchedSiplet.getSipApp();
				
				initSipApp(appDesc);
				return matchedSiplet;
			}
			return null;
		}

		// JSR 289 get the next application by application
		// router strategy
		if (request.getDirective() == null) {
			request.setDirective(SipApplicationRoutingDirective.NEW);
		}

		SipServletDesc matchedSiplet = null;
		SipApplicationRouterInfo appInfo = null;
		
		//We must set the popped route before getting to the application router code. 
		if(request instanceof IncomingSipServletRequest)
			request.checkTopRouteDestination(true);
		
		// Look up for match siplet 
		while (null == matchedSiplet) {

			try {
				if (c_logger.isTraceDebugEnabled()) {
					StringBuffer buff = new StringBuffer();
					buff.append("\n looking for next application. Calling applicationRouter.getNextApplication() with the following arguments:  \n")
						.append("applicationRouter= " + applicationRouter)
						.append(", request.callID=" + request.getCallId())
						.append(", request.getRegion()=" + request.getRegion())
						.append(", directive=" + request.getDirective())
						.append(", sipTargetedRequestInfo=" + sipTargetedRequestInfo)
						.append(", request.getStateInfo()="+ request.getStateInfo());
					c_logger.traceDebug(this, "findSippletMatch",
							buff.toString());
					
				}
				
				// Ask application router to mount next application
				appInfo = applicationRouter.getNextApplication(request, request.getRegion(),
						request.getDirective(), sipTargetedRequestInfo, request.getStateInfo());

				// Application router should not return null
				if (appInfo == null) {
					c_logger.traceDebug(this, "findSippletMatch",
							"Application router return null");
					
					return null;
				}
				
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "findSippletMatch",
							"appinfo: " + appInfo);
				}
				
				// Manage routing for Outgoing or Incoming SipServletRequest 
				// the message will be sent to the URI specified by appInfo.getRoute(), if it is external 
				 if( appInfo.getRouteModifier() != null &&
					(appInfo.getRouteModifier().equals(SipRouteModifier.ROUTE) ||
					appInfo.getRouteModifier().equals(SipRouteModifier.ROUTE_BACK))) {

					routeRequest(request, appInfo);
					if( request.isExternalRoute()){
						return null;
					}
				}
				// Save data passed by application router in the request.
				// That data will be saved in the TransactionUserWrapper
				// when the last one will be ready.
				saveAppInfoDataInTheRequest(request, appInfo);

			} catch (Throwable e) {

				if (c_logger.isTraceDebugEnabled())
					c_logger.traceDebug(this, "findMatch",
							"Error in application router", e);

				request.processCompositionErrorResponse();
				return null;
			}

			// next time send CONTINUE directive 
			if (request.getDirective().equals(
					SipApplicationRoutingDirective.NEW)) {
				request.setDirective(SipApplicationRoutingDirective.CONTINUE);
			}

			// End of applications chain
			if ((appInfo.getNextApplicationName() == null) || (appInfo.getNextApplicationName().equals(""))) {
				//According to JSR 289 15.4.1: if there are one or more  
				//Route headers, the request needs to be sent externally.
				if (request.getHeader(RouteHeader.name) != null && 
					PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.WAS80_ROUTE_WHEN_NO_APPLICATION)) {
						request.setExternalRoute(true);
				}
				return null;
			}

			request.setStateInfo(appInfo.getStateInfo());

			matchedSiplet = sipletMatcher.matchSipletForApplication(request,
					appInfo.getNextApplicationName());
			
			// An error in the matcher can cause a response to be sent.
			if (request.isCommitted())return null;

		}

		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "findSippletMatch");
		}

	
		SipAppDesc appDesc = matchedSiplet.getSipApp();
		//init app if needed
		initSipApp(appDesc);
		return matchedSiplet;
	}


	/**
	 * Application stopped  
	 * @param appName - application to unload
	 * @return
	 */
	public SipAppDesc unloadApplicationConfiguration(String appName) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "unloadApplicationConfiguration",
					"unloadApplicationConfiguration name: " + appName);
		}

		List<String> undeployedApplications = new ArrayList<String>();
    	undeployedApplications.add(appName);
    	applicationRouter.applicationUndeployed(undeployedApplications);
		
		return sipletMatcher.unloadApplicationConfiguration(appName);
	}

	public SipServletDesc getDefaultHandler() {
		return sipletMatcher.getDefaultHandler();
	}

	
    /**
     * Gets a Sip Servlet description object according to the siplet's names. Searches
     * through all application and tries to find matching siplet.   
     * @param name The name of the siplet as appears in the sip.xml file. 
     * @return The matching Sip Servlet Descriptor if available otherwise null. 
     */
	public SipServletDesc getSipletByName(String name) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getSipletByName",
					"getSipletByName name: " + name);
		}
		return sipletMatcher.getSipletByName(name);
	}

    /**
     * Gets the SIP App descriptor for the given application name. 
     * @param appName The name of the SIP Application. 
     * @return The SIP App Descriptor if available, otherwise null
     */
	public SipAppDesc getSipApp(String appName) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "getSipApp",
					"getSipApp name: " + appName);
		}
		return sipletMatcher.getSipApp(appName);
	}

    /**
     * Gives number of running SIP applications
     * @return
     */
	public int getNumOfRunningApplications() {
		return sipletMatcher.getNumOfRunningApplications();
	}


	/**
	 *
	 * Application info contains data that should be saved in the session, 
	 * the problem is that the session does not exist yet. The data is created 
	 * temporarily in the request, and when the session will be created
	 * the data will be assigned to it.
	 * 
	 * @param incomingRequest 
	 * @param appInfo
	 */
	private void saveAppInfoDataInTheRequest(
			SipServletRequestImpl request,
			SipApplicationRouterInfo appInfo) {

		if (appInfo.getSubscriberURI() != null) {
			String subscriberURIString = appInfo.getSubscriberURI();
			URI subscriberURI = null;

			try {
				SipFactory sipFactory = SipServletsFactoryImpl.getInstance();
				subscriberURI = sipFactory.createURI(subscriberURIString);
			} catch (ServletParseException e) {
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "findMatch",
							"Unable to construct URI from the subscriberURI sting - "
									+ subscriberURIString);
				}
			}
			request.setSubscriberURI(subscriberURI);
		}

		if (appInfo.getRoutingRegion() != null) {
			request.setRoutingRegion(appInfo.getRoutingRegion());
		}

/*
		// If the route modifier is NO_ROUTE remove first route header
		if (appInfo.getRouteModifier() == SipRouteModifier.NO_ROUTE) {
			//    		 clean the saved popped pouted header
			request.setPoppedRoute(null);

//			request.getMessage().removeHeader(RouteHeader.name, true);
			request.removeHeader(RouteHeader.name, false);
		}
 */		
	}
    
    /**
     * Handling the route addresses returned from the application router info, 
     * according to JSR 289 15.4.1
     * @param request
     * @param appInfo
     */
    private void routeRequest(SipServletRequestImpl request, 
    		SipApplicationRouterInfo appInfo){
    	if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "routeRequest", new Object[]{request, appInfo}); 
		}
		String[] routeArray = appInfo.getRoutes();
		if (routeArray.length == 0) return;
		
		String route = null;
		
		request.cleanExpiredCompositionHeaders();
		
		route = routeArray[0];
		//check the first route first.
		if( isFirstRouteInternal(request, route, appInfo)){
			//First route was internal, ignoring the others if any 
			return;
		}
		
		//push the local container route first in case of a ROUTE_BACK, 
		//the request will be routed back to the container after visiting in all the 
		//routes
		if (appInfo.getRouteModifier().equals(SipRouteModifier.ROUTE_BACK)){
			SipURI routeUri = SipUtil.creatLocalRouteHeader(request, appInfo.getStateInfo());
			
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "routeRequest", "AR returned ROUTE_BACK, adding local container route header value: " + routeUri);
    		}
			
			if (routeUri != null){
				request.pushRoute(routeUri);
			}
		}
		
		for (int i = routeArray.length -1; i >= 0; --i){
			//Pushing any external route or internal (as long as the first one wasn't internal)
			//Making sure first one is on top and pushing it last.
			route = routeArray[i];
			handleAllRoutes(request, route, appInfo);
		}
		request.setExternalRoute(true);
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceExit(this, "routeRequest"); 
		}
    }
    
    /**
     * Handle all the routes returned from the application router
     * this method will be called only if first route was external.
     * @param request
     * @param route
     * @param appInfo
     */
    private void handleAllRoutes( SipServletRequestImpl request, 
			  					  String route, 
			  					  SipApplicationRouterInfo appInfo){
    	handleRoute(false, request, route, appInfo);
    }
    /**
     * Verify and handle first route returned from Application router
     * @param request
     * @param route
     * @param appInfo
     * @return true if route was internal, false otherwise.
     */
    private boolean isFirstRouteInternal( 
			  SipServletRequestImpl request, 
			  String route, 
			  SipApplicationRouterInfo appInfo){
    	return handleRoute( true, request, route, appInfo);
    }
    
    /**
     * Check whether the first route is internal and if so pass it to the poppedRoute method and 
     * return false
     * @param sipRouteUri
     * @param request
     * @return
     */
    private boolean handleFirstRoute( SipURIImpl sipRouteUri,
    								  SipServletRequestImpl request,
    								  Address routeAddress){
    	String host = sipRouteUri.getHost();
        int port = sipRouteUri.getPort();
        boolean secure = sipRouteUri.getScheme().equalsIgnoreCase("sips");
        //Checking for container listening point or SLSP proxy listening point
        if ( request.checkIsLocalListeningPoint(host, port, secure)) {
        	if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleRoute",
						"Internal Route: "
								+ sipRouteUri + " pushed to top route of request callID=" + 
								request.getCallId() + ", method= " + 
								request.getMethod() + ", request object= \n"+ this);
			}
    	
    		if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleRoute",
						"First route is internal, making available in popped route and ignoring the rest");
    		}
    		
    		request.setPoppedRoute(routeAddress);
    		return true; 
    		//if the first route is internal then we need to make it available 
    		//in the popped route and ignore the other routes (JSR 289 15.4.1}
    	}
        return false; //first route was internal
    }
    
    /**
     * HAndle a route received from the application router
     * @param checkFirstRoute
     * @param request
     * @param route
     * @param appInfo
     * @return true if route is external, false if the first route is local
     */
	private boolean handleRoute( boolean checkFirstRoute, 
								  SipServletRequestImpl request, 
								  String route, 
								  SipApplicationRouterInfo appInfo){
		if (c_logger.isTraceEntryExitEnabled()) {
			c_logger.traceEntry(this, "handleRoute", 
					new Object[]{checkFirstRoute, request, route, appInfo}); 
		}
		Address routeAddress = null;
    	SipURIImpl sipRouteUri = null;
    	try{
	    	SipFactory sipFactory = SipServletsFactoryImpl.getInstance();
			routeAddress = sipFactory.createAddress(route);
			
			if( !(routeAddress.getURI() instanceof SipURIImpl)){
				throw new IllegalArgumentException(
						"Application router info is invalid. Route doesn't contain valid SIP URI " 
						+ routeAddress);
			}
			
			sipRouteUri = (SipURIImpl)routeAddress.getURI();
			
			if(checkFirstRoute){
				return handleFirstRoute(sipRouteUri, request, routeAddress);
	        }
        	//If first route was external, all routes (including other internals), needs to be pushed
        	request.pushRoute(sipRouteUri);
        	
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleRoute",
						"External Route: "
								+ sipRouteUri + " pushed to top route of request callID=" + 
								request.getCallId() + ", method= " + 
								request.getMethod() + ", request object= \n"+ this);
			}
	        return true;
    	} catch (ServletParseException e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "handleRoute",
						"Unable to construct URI from the route URI string "
								+ sipRouteUri);
			}
			throw new IllegalArgumentException(
					"Application router info is invalid. Route doesn't contain valid SIP URI " 
					+ routeAddress);
		}
    	finally{
	    	if (c_logger.isTraceEntryExitEnabled()) {
				c_logger.traceExit(this, "handleRoute"); 
			}
    	}
	}

	/**
	 * Load the application configuration for the sip application that match the Webpp
	 * @param webApp
	 */
	public void loadAppConfiguration(WebApp webApp ) {
		
		List<String> deployedApplications = new ArrayList<String>();
    	
		SipAppDesc app = sipAppDescManager.getSipAppDesc(webApp);
		
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "loadAppConfiguration",
					"loadAppConfiguration App: " + app);
		}
		
		deployedApplications.add(app.getApplicationName());
    	applicationRouter.applicationDeployed(deployedApplications);
    	
		 this.sipletMatcher.loadAppConfiguration(sipAppDescManager.getSipAppDesc(webApp));
	}

	/**
	 * Sends the list of deployed applications to the SipApplicationRouter and load the configuration for all the applications
	 */
	public void notifyRouterOnDeployedApps() {

    	for(SipAppDesc appDesc : sipAppDescManager.getSipAppDescs()) {
    		this.sipletMatcher.loadAppConfiguration(appDesc);
    	}
    	applicationRouter.applicationDeployed(sipAppDescManager.getSipAppNames());
		
	}
	
	private void initSipApp(SipAppDesc appDesc) {	
			//init app if needed
			try {
				sipAppDescManager.initSipAppIfNeeded(appDesc.getWebAppName());
			} catch (ServletException e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc,"initSipApp Failed to initalized application: "+ appDesc.getApplicationName() , e.getLocalizedMessage());
				}
			} catch (Throwable e) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
					Tr.debug(tc,"initSipApp Failed to initalized application: "+ appDesc.getApplicationName() , e.getLocalizedMessage());
				}
			}

		}
	
}
