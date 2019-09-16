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
package com.ibm.ws.sip.container.router;

import jain.protocol.ip.sip.SipPeerUnavailableException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration.Dynamic;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipURI;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.websphere.servlet.container.WebContainer;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.appqueue.MessageDispatchingException;
import com.ibm.ws.sip.container.asynch.AsynchronousWorkTaskFactory;
import com.ibm.ws.sip.container.failover.repository.SessionRepository;
import com.ibm.ws.sip.container.osgi.AsynchronousWorkHolder;
import com.ibm.ws.sip.container.osgi.ServletContextManager;
import com.ibm.ws.sip.container.osgi.ServletInstanceHolderFactory;
import com.ibm.ws.sip.container.parser.ServletsInstanceHolder;
import com.ibm.ws.sip.container.parser.SipAppDesc;
import com.ibm.ws.sip.container.parser.SipServletDesc;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.proxy.SipProxyInfo;
import com.ibm.ws.sip.container.servlets.ServletContextFactoryImpl;
import com.ibm.ws.sip.container.util.SipLogExtension;
import com.ibm.ws.sip.container.was.WASContextEstablisher;
import com.ibm.ws.sip.container.was.WASHttpSessionListener;
import com.ibm.ws.sip.container.was.filters.SipFilter;
import com.ibm.ws.webcontainer.webapp.WebApp;
import com.ibm.wsspi.webcontainer.servlet.IServletContext;
import com.ibm.wsspi.webcontainer.util.ThreadContextHelper;

/**
 * This class is a data structure and handler for the SipAppDesc and WebApp object.
 * The class is needed for lazy initialization in Liberty to connect the SipAppDesc and WebApp when needed
 * and provide translation between them while enabling the initialization of a WebApp when requested.
 * @author SAGIA
 *
 */
public class SipAppDescManager {

	private final ReentrantLock appLock = new ReentrantLock();
	/**
	 * Set to true when the first SIP application is started.
	 * We use so that the SIP container will be initialized only if
	 * a SIP application was installed and started
	 */
	private static boolean firstSipAppStarted = false;	

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger =
			Log.get(SipAppDescManager.class);

	/**
	 * Singleton instance
	 */
	private static SipAppDescManager s_instance =  new SipAppDescManager();

	/**
	 * Mapping from SipAppDesc to webApp name (the name of the application in the Web Container context). 
	 */
	private ConcurrentHashMap<String, SipAppDesc> _applications;

	/**
	 * Return the instance of the class create a new one if no instance exists
	 * @return
	 */
	public static SipAppDescManager getInstance(){

		return s_instance;
	}


	/**
	 * Ctor. Initialize the map
	 */
	private SipAppDescManager(){
		_applications = new ConcurrentHashMap<String, SipAppDesc>();
	}


	/**
	 * Adds new SipWebApp to the map
	 * @param appDesc the Sip application descriptor
	 * @return
	 */
	public SipAppDesc addNewApp(SipAppDesc appDesc) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "addNewApp", "Adding new application: Name = " + appDesc.getWebAppName() + " Desc = " + appDesc.getDescription());
		}

		//initializing container (will happen only for the first app)
		SipContainer sipCon = SipContainer.getInstance();
		sipCon.init();

		return _applications.put(appDesc.getWebAppName(), appDesc);
	}

	/**
	 * Adds the webApp to the matching SipAppDesc and setting additional attributes on the AppDesc from the new information
	 * @param webApp WebApp object from the Web Container.
	 * @return
	 */
	public SipAppDesc updateWebApp(WebApp webApp) {

		String name = webApp.getName();
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "updateWebApp", "Name = " + name);
		}
		SipAppDesc appDesc = _applications.get(name);
		if(appDesc != null) {
			appDesc.setWebApp(webApp);
			return appDesc;
		}
		else {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(null, "updateWebApp cannot find Sip Application", name);
			}
			return null;
		}
	}


	/**
	 * Initialize the webApp which loads the application and activate it.
	 * In addition the contextEstablisher is created and set and a LifeCycleListener is registered with the WebApp for application composition.
	 * @param name the application name
	 * @throws ServletException
	 * @throws Throwable
	 */
	public void initSipAppIfNeeded(String name) throws ServletException, Throwable {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "initSipAppIfNeeded", "Name = " + name);
		}
		SipAppDesc appDesc = _applications.get(name);

		if(appDesc == null) {
			if (c_logger.isWarnEnabled()) {
				c_logger.warn("initSipAppIfNeeded cannot find Sip Application", null, name);
			}
			return;	
		}
		if(appDesc.getContextEstablisher() != null) {
			return;
		}

		appLock.lock();
		try{
			if (appDesc.wasInitialized()) {
				return;
			}
			appDesc.setWasInitialized(true);
			initContainerComponentsOnFirstApp();
			setWebContainerConfig(appDesc);


		}
		finally{
			appLock.unlock();
		}

		// Initialize / access WebContainer outside of SIP appLock
		// block to prevent deadlock between SIP and Webcontainer 

		WebApp webApp = appDesc.getWebApp();
		WASHttpSessionListener httpSessionListener = null;

		//webApp initialization 
		if(!webApp.isInitialized() && !appDesc.isDuringWebAppInitialization()) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(null, "initSipAppIfNeeded", "init WebApp " + webApp);
			}

			//TODO Need to check that in converged application where the first event is 
			// an HTTP incoming message, the SIP container initialization still happens
			webApp.initialize();
		}
		//if the contextEstablisher is null then we didn't set the needed attributes on the appDesc
		//And the application was initialized by the WebContainer
		if(appDesc.getContextEstablisher() == null) {
			appDesc.setRootURI( webApp.getContextPath());
			appDesc.setIsDistributed( webApp.getWebAppConfig().isDistributable());
			_applications.get(name).setContextEstablisher(new WASContextEstablisher(webApp));
			httpSessionListener = SipContainer.getHttpSessionListener();
			appDesc.setupSipApplication();
		}

		if (httpSessionListener == null) {
			throw new RuntimeException("Unable to add http session listener");
		}
		else {
			webApp.addLifecycleListener(httpSessionListener);
		}	

	}

	/**
	 * Creates a filter configuration object.
	 * 
	 * @return filter configuration object
	 */
	private EnumSet<DispatcherType> getFilterTypes() {

		ArrayList<DispatcherType> types = new ArrayList<DispatcherType>(5);
		types.add(DispatcherType.ASYNC);
		types.add(DispatcherType.ERROR);
		types.add(DispatcherType.FORWARD);
		types.add(DispatcherType.INCLUDE);
		types.add(DispatcherType.REQUEST);
		return EnumSet.copyOf(types);

	}

	/**
	 * Creates and registers a filter for pre-processing requests before they are
	 * served to the siplet.
	 */
	private void createServletsFilter(SipAppDesc sipApp) {

		Iterator<SipServletDesc> itr = sipApp.getSipServlets().iterator();
		IServletContext servletContext = sipApp.getWebApp();
		//all servlets will be accessed through this filter. 
		Dynamic filterReg = servletContext.addFilter("SipFilter", SipFilter.class.getName());
		EnumSet<DispatcherType> filterTypes = getFilterTypes();
		while (itr.hasNext())
		{
			SipServletDesc desc = itr.next();
			String name = desc.getName();

			filterReg.addMappingForServletNames(filterTypes, false, name);
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createServletsFilter", "pattern = " + name);
			}
		}

		try {
			List<SipURI> outboundIfList = SipProxyInfo.getInstance().getOutboundInterfaceList();
			if (outboundIfList != null) {
				servletContext.setAttribute(SipServlet.OUTBOUND_INTERFACES, outboundIfList);
			}
			else {
				servletContext.setAttribute(SipServlet.OUTBOUND_INTERFACES, new LinkedList<Object>());

			}	
		} catch (Exception e) {
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "createServletsFilter", "Error setting the Outbound Interfaces" + e.getMessage());
			}


		}

	}

	/**
	 * Add SIP components to the Web application context and configuration
	 * @param appDesc
	 */
	private void setWebContainerConfig(SipAppDesc appDesc) throws ServletException{
		if (c_logger.isTraceDebugEnabled()) 
		{
			c_logger.traceDebug("setWebContainerConfig: SIP Application: " + appDesc);
		}

		createServletsFilter(appDesc);
	}

	/**
	 * Starting SIP container
	 * 
	 * @throws IllegalStateException
	 * @throws SipPeerUnavailableException
	 * @throws MessageDispatchingException
	 */
	private void startSipContainer() throws IllegalStateException, SipPeerUnavailableException, MessageDispatchingException {
		if (c_logger.isTraceEntryExitEnabled()) 
		{
			c_logger.traceEntry(null, "startSipContainer");
		}

		// Init SipServlet for interface bundle
		ServletInstanceHolderFactory.setInstanceHolder(ServletsInstanceHolder.getInstance());

		// Init asynchronous worker
		AsynchronousWorkHolder.setAsynchWorkInstance(new AsynchronousWorkTaskFactory());

		SipContainer.getInstance().getMessageDispatcher().start();

		ServletContextManager.
		getInstance().
		setContextFactory(new ServletContextFactoryImpl());

		//initialize the session repository
		SessionRepository.getInstance();

		//Init the HPEL log extension for adding SIP info to it
		SipLogExtension.init();

		


		if (c_logger.isTraceEntryExitEnabled()) 
		{
			c_logger.traceExit(null, "startSipContainer");
		}
	}

	/**    
	 * Initialize container component when the first SIP application is started
	 */
	private void initContainerComponentsOnFirstApp() {
		if( !firstSipAppStarted) {
			firstSipAppStarted = true;
			try {
				//initAndStartHAComponents();
				/*
				 * TODO Liberty pmi - initialize performancemgr
				 * PerformanceMgr.setPmiModuleFactory( new WASPMIModuleFactory());
				 * 
				 * PerformanceMgr.getInstance().init(false,
				 * PropertiesStore.getInstance().getProperties());
				 */
				startSipContainer();
				/* TODO Liberty MessageContext.setPerfMgr(PerformanceMgr.getInstance()); */
			} catch (Throwable e) {
				if (c_logger.isErrorEnabled()) {
					c_logger.error("error.initialize.sip.container", Situation.SITUATION_START,
							null, e);
				}
				return;
			}
		} else {
			if (SipContainer.getInstance().getNumOfRunningApplications() == 1) {
				//The container is already up and this is the first app,
				//meaning - last app stopped and another (or the same) app started
				if (c_logger.isTraceDebugEnabled()) {
					c_logger.traceDebug(this, "starting", "Recreating PMI timers");
				}

				// Init the HPEL log extension for adding SIP info to it
				SipLogExtension.init();

				PerformanceMgr perfMgr = PerformanceMgr.getInstance();
				if (perfMgr != null) {
					//Recreate PMI timers (defect 677439) 
					perfMgr.createPMITimer();

					if (perfMgr.getCurrentServerWeight() == 0) {
						//this is first application, but container already up and its weight is 0 
						//means last application stopped and another (or the same) application started

						perfMgr.setServerWeight(-1); //Make the container available again with the 
						//dynamic weight that was calculated . Fix for 387747 
						if (c_logger.isTraceDebugEnabled()) {
							c_logger.traceDebug(this, "starting", "returning server weight to dynamic status. new weight: "
									+ perfMgr.getCurrentServerWeight());
						}
					}
				}
			}
		}
	}


	/**
	 * Return all the applications 
	 * @return
	 */
	public Collection<SipAppDesc> getSipAppDescs() {
		return _applications.values();
	}


	/**
	 * Return all the application names - the name of the sip application
	 * @return
	 */
	public List<String> getSipAppNames(){
		ArrayList<String> appNames = new ArrayList<String>();
		for(SipAppDesc appDesc : _applications.values()) {
			appNames.add(appDesc.getApplicationName());
		}

		return appNames;

	}

	/**
	 * Get the SipAppDesc matching the webApp or null otherwise
	 * @param webApp
	 * @return
	 */
	public SipAppDesc getSipAppDesc(WebApp webApp) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "getSipAppDesc", "Name = " + webApp.getName());
		}
		return _applications.get(webApp.getName());
	}

	/**
	 * Get the SipAppDesc matching the webApp or null otherwise
	 * @param webApp
	 * @return
	 */
	public SipAppDesc getSipAppDesc(String webAppName) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "getSipAppDesc", "Name = " + webAppName);
		}
		return _applications.get(webAppName);
	}

	/**
	 * Remove an application.
	 * @param webApp
	 */
	public void removeApp(WebApp webApp){
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "removeApp", "Remove application: Name = " + webApp.getName());
		}
		_applications.remove(webApp.getName());
	}

	/**
	 * return SipAppDesc for the application requested
	 * @param name the application name of the application
	 * @return
	 */
	public SipAppDesc getSipAppDescByAppName(String name) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "getSipAppDescByAppName", "getSipAppDescByAppName Name = " + name);
		}
		for(SipAppDesc appDesc : _applications.values()) {
			if(name.equals(appDesc.getApplicationName())) {
				return appDesc;
			}
		}
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(null, "getSipAppDescByAppName", "getSipAppDescByAppName No Application was found with the name " + name);
		}
		return null;

	}
}
