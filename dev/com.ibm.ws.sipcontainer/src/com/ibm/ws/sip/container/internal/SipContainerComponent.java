/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.internal;

import jain.protocol.ip.sip.SipStack;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.sip.ar.SipApplicationRouter;
import javax.xml.parsers.ParserConfigurationException;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.SipContainer;
import com.ibm.ws.sip.container.appqueue.MessageDispatchingHandler;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.container.protocol.SipProtocolLayer;
import com.ibm.ws.sip.container.resolver.DomainResolverImpl;
import com.ibm.ws.sip.container.router.SipRouter;
import com.ibm.ws.sip.container.timer.BaseTimerService;
import com.ibm.ws.sip.container.was.WebsphereLauncherImpl;
import com.ibm.ws.sip.container.was.message.SipMessageFactory;
import com.ibm.ws.sip.stack.transport.chfw.GenericEndpointImpl;
import com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;

/**
 * A declarative services component.
 * The component is responsible of initiating the container.
 * Is injected when a first application is deployed.
 * 
 */
@Component(service = SipContainerComponent.class,
configurationPolicy = ConfigurationPolicy.OPTIONAL,
configurationPid = "com.ibm.ws.sip.container.internal.SipContainerComponent",
name = "com.ibm.ws.sip.container.internal.SipContainerComponent",
property = {"service.vendor=IBM"} )
public class SipContainerComponent {
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipContainerComponent.class);


 	/** Reference for delayed activation of Sip Application Router */
 	private static final AtomicServiceReference<SipApplicationRouter> s_sipApplicationRouterSvcRef = new AtomicServiceReference<SipApplicationRouter>("com.ibm.ws.sip.ar");

	/** Message dispatching handler service reference -- required */
	private static MessageDispatchingHandler s_messageDispatchingHandlerSvc = null;
	
	/** SIP container service for creating timers*/
	private static BaseTimerService s_timerService;
	
	/** Reference for delayed activation of SipStack */
	private static final AtomicServiceReference<SipStack> s_sipStackSvcRef = new AtomicServiceReference<SipStack>("com.ibm.ws.jain.protocol.ip.sip");
	
	/** Reference for delayed activation of SipStack */
	private static final AtomicServiceReference<PerformanceMgr> s_perfManager = new AtomicServiceReference<PerformanceMgr>("com.ibm.ws.sip.container.pmi");
	
	/** Use the AtomicServiceReference class to manage set/unset/locate/cache of the ServletContainerInitializerExtensions */
    private static final ConcurrentServiceReferenceSet<GenericEndpointImpl> genericEndpointRef = new ConcurrentServiceReferenceSet<GenericEndpointImpl>("com.ibm.ws.sip.endpoint");

	/** Use the AtomicServiceReference class to manage set/unset/locate/cache of the SipMessageFactories */
    private static final ConcurrentServiceReferenceSet<SipMessageFactory> sipMessageFactoryRef = new ConcurrentServiceReferenceSet<SipMessageFactory>("com.ibm.ws.sip.container.was.message.SipMessageFactory");

	/** Uses for creating suitable SipMessage */
    private static SipMessageFactory _sipMessageFactory = null;
    
	/** Holds the component context received in the component activate method */
	private static ComponentContext m_context = null;

	/**SIP container launcher*/
	private WebsphereLauncherImpl wsl = new WebsphereLauncherImpl();
	
	/**DS for managing VHs, used in the SIP application selection process*/
	private static DynamicVirtualHostManager _vhostManager;
	
    /** Indicates whether the SIP router is initialized  */
	private static boolean s_initialized = false;


	/**SIP Container OSGi bundle context*/
	public static ComponentContext getContext() {
		return m_context;
	}
	
	private static DomainResolverImpl _domainResolver;
	/**
	 * DS method to activate this component.
	 * 
	 * @param	context 	: Component context of the component 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 */
	protected void activate(ComponentContext context, Map<String, Object> properties) {
		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipContainerComponent activated", properties);
		m_context = context;
		try {
			PropertiesStore.getInstance().getProperties().updateProperties(properties);
			initContainer();
		} catch (ParserConfigurationException e) {
			if (c_logger.isErrorEnabled())
				c_logger.error("error.initialize.sip.container");
		}
		
		genericEndpointRef.activate(context);
		sipMessageFactoryRef.activate(context);
	}

	/**
	 * init the container
	 * 
	 */
	private void initContainer() throws ParserConfigurationException  {
		wsl.init();
		if (c_logger.isTraceDebugEnabled() ) {
			c_logger.traceDebug("initContainer done");
		}

	}

	/**
	 * DS method to deactivate this component.
	 * 
	 * @param reason int representation of reason the component is stopping
	 */
	protected void deactivate(int reason) {
		if (c_logger.isEventEnabled())
			c_logger.event("SipContainerComponent deactivated, reason=" + reason);
		wsl.stop();
		s_sipStackSvcRef.deactivate(m_context);
		genericEndpointRef.deactivate(m_context);
		sipMessageFactoryRef.deactivate(m_context);
		s_perfManager.deactivate(m_context);
	}

	/**
	 * DS method to modify this component.
	 * 
	 * @param properties : Map containing service & config properties
	 *            populated/provided by config admin
	 */
	@Modified
	protected void modified(Map<String, Object> properties) {
		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipContainerComponent modified", properties);
		PropertiesStore.getInstance().getProperties().updateProperties(properties);
	}
	
	/**
     * DS method for setting the Message Dispatching Handler service reference.
     * 
     * @param MessageDispatchingHandler
     */
	@Reference(service=MessageDispatchingHandler.class, policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	public void setMessageDispatchingHandlerSvc(MessageDispatchingHandler messageDispatchingHandlerSvc) { 
		s_messageDispatchingHandlerSvc = messageDispatchingHandlerSvc;
	}

	/**
    * DS method for removing the Message Dispatching Handler service reference.
    * 
    * @param MessageDispatchingHandler
    */
	public void unsetMessageDispatchingHandlerSvc(MessageDispatchingHandler MessageDispatchingHandlerSvc) {
		if (s_messageDispatchingHandlerSvc == MessageDispatchingHandlerSvc) {
			s_messageDispatchingHandlerSvc = null;
		}
	}

	/**
    * Access to the MessageDispatchingHandler
    * 
    * @return SipApplicationRouter
    */
	public static MessageDispatchingHandler getMessageDispatchingHandlerSvc() {
		return s_messageDispatchingHandlerSvc;
	}
	
	/**
	 * Getting timer service singleton 
	 * @return
	 */
	public static BaseTimerService getTimerService() {
		return s_timerService;
	}

	/**
	 * Setting timer service DS
	 * @param s_timerService
	 */
	@Reference
	public void setTimerService(BaseTimerService timerService) {
		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipContainerComponent setTimerService", timerService);
		SipContainerComponent.s_timerService = timerService;
	}
	
	/**
	 * Unsetting the timer service DS
	 * @param timerService
	 */
	public void unsetTimerService(BaseTimerService timerService) {
		if(timerService == s_timerService) SipContainerComponent.s_timerService = null;
	}


	
    /** Required static reference: called before activate */
    @Reference(name = "com.ibm.ws.webcontainer.osgi.DynamicVirtualHostManager", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
    protected void setVirtualHostMgr(DynamicVirtualHostManager vhostMgr) {
        _vhostManager = vhostMgr;
     
   }


    /**
     * unset the dynamic virtual host manger used to selecting the dynamic virutal host
     */
    protected void unsetVirtualHostMgr(DynamicVirtualHostManager vhostMgr) {
    	if(vhostMgr == _vhostManager) {
    		_vhostManager = null;
    	}
    }
  
    public static DynamicVirtualHostManager getVirtualHostMgr() {
		return _vhostManager;
	}
	
    /**
     * set the Sip message factory. used to create sip message objects.
     */    @Reference( service=SipMessageFactory.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.AT_LEAST_ONE, name ="com.ibm.ws.sip.container.was.message.SipMessageFactory")
    protected void setSipMessageFactory(ServiceReference<SipMessageFactory> ref) {
	 	if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug( "setSipMessageFactory", ref);
		}
	 	
    	sipMessageFactoryRef.addReference(ref);
    	updateCurrentSipMessageFactory();
   }
    
    /**
     * unset the Sip message factory. used to create sip message objects.
     */
    protected void unsetSipMessageFactory(ServiceReference<SipMessageFactory> ref) {
    	if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug( "unsetSipMessageFactory", ref);
		}
    	
    	sipMessageFactoryRef.removeReference(ref);
    	updateCurrentSipMessageFactory();
    }
  
    /**
     * Updates current SipMessageFactory for the sip container.
     * It assumes the 31 factory has higher ranking.
     */
    private synchronized static void updateCurrentSipMessageFactory(){
    	SipMessageFactory currentHighestFactory = sipMessageFactoryRef.getHighestRankedService();

    	if(_sipMessageFactory != currentHighestFactory){
        	_sipMessageFactory = currentHighestFactory;
	    	if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug("updateCurrentSipMessageFactory updates SipMessageFactory", _sipMessageFactory);
			}
    	}
    }
    
    /**
     * set the Sip message factory. used to create sip message objects.
     */
    public static SipMessageFactory getSipMessageFactory() {
    	if(_sipMessageFactory == null){
    		updateCurrentSipMessageFactory();
    	}
    	
    	return _sipMessageFactory;
	}
    
    /**
     * DS method for setting the SipStack service reference.
     * 
     * @param service
     */
	@Reference(service=PerformanceMgr.class, name="com.ibm.ws.sip.container.pmi", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	protected void setPerformanceManager(ServiceReference<PerformanceMgr> perfManager) {
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("setPerformanceManager", perfManager);
		}
		s_perfManager.setReference(perfManager);

	}
	/**
     * DS method for unsetting the GenericEndpoits service reference.
     * 
     * @param service
     */
    protected void unsetPerformanceManager(ServiceReference<PerformanceMgr> perfManager) {
    	if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug( "unsetPerformanceManager", perfManager);
		}
    	s_perfManager.unsetReference(perfManager);
    }
	
	/**
     * DS method for setting the SipStack service reference.
     * 
     * @param service
     */
	@Reference(service=SipStack.class, name="com.ibm.ws.jain.protocol.ip.sip", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	public void setSipStack(ServiceReference<SipStack> sipStack) {
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("setSipStack", sipStack);
		}
		s_sipStackSvcRef.setReference(sipStack);

	}

	/**
     * DS method for setting the GenericEndpoits service reference.
     * 
     * @param service
     */
	@Reference(service=GenericEndpointImpl.class, cardinality=ReferenceCardinality.AT_LEAST_ONE, policy=ReferencePolicy.DYNAMIC, name="com.ibm.ws.sip.endpoint")
    protected void setGenericEndpoint(ServiceReference<GenericEndpointImpl> ref) {
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug( "setGenericEndpoint", ref);
		}
		genericEndpointRef.addReference(ref);
		
		if(SipProtocolLayer.getInstance().isInitialized()){
			startGenericEndpoints();
		}//otherwise they will get initialized when the SipProtocolLayer.init is done
    }

	/**
     * DS method for unsetting the GenericEndpoits service reference.
     * 
     * @param service
     */
    protected void unsetGenericEndpoint(ServiceReference<GenericEndpointImpl> ref) {
    	if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug( "unsetGenericEndpoint", ref);
		}
    	genericEndpointRef.removeReference(ref);
    }
    
    /**
     * Return iterator over GEP services
     * @return
     */
    private static Iterator<GenericEndpointImpl> getGenericEndpoints() {
    	return genericEndpointRef.getServices();
    }
    
    /**
     * Starting all GenericEndpoints services.
     * 
     * @param service
     */
    public static void startGenericEndpoints(){
    	Iterator<GenericEndpointImpl> gepi = SipContainerComponent.getGenericEndpoints();
		if(gepi == null || !gepi.hasNext()){
			if (c_logger.isTraceDebugEnabled()){
				c_logger.traceDebug( "startGenericEndpoints", "GEP service was not set");
			}
		}else{
			while(gepi.hasNext()){
				gepi.next();
			}
		}
    }
    
    
    
	/**
     * DS method for removing the SipStack service reference.
     * 
     * @param service
     */
	public void unsetSipStackService(ServiceReference<SipStack> sipStackSvc) {
		if (c_logger.isTraceDebugEnabled()){
			c_logger.traceDebug("unsetSipStack", sipStackSvc);
		}
		s_sipStackSvcRef.unsetReference(sipStackSvc);
	}
	
	/**
     * Access to the SipStack service
     * 
     * @return AtomicServiceReference<SipStack>
     */
	public static AtomicServiceReference<SipStack> getSipStackService() {
		return s_sipStackSvcRef;
	}

	/**
     * Activates SipStack service reference
     * 
     * @return AtomicServiceReference<SipStack>
     */
	public static void activateSipStack() {
		s_sipStackSvcRef.activate(m_context);
		
	}
	
	/**
     * Activates PerfManager service reference
     * 
     * @return AtomicServiceReference<PerfManager>
     */
	public static void activatePerfManager() {
		s_perfManager.activate(m_context);
		
	}
	
	/**
     * DS method for setting the SipStack service reference.
     * 
     */
	@Reference(service=SipApplicationRouter.class, name="com.ibm.ws.sip.ar", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
	public void setSipApplicationRouter(ServiceReference<SipApplicationRouter> sipApplicationRouter) {
	     if (c_logger.isTraceDebugEnabled()) {
	            c_logger.traceDebug("setSipApplicationRouter: "+sipApplicationRouter);
	     }
		s_sipApplicationRouterSvcRef.setReference(sipApplicationRouter);
		
	}

	/**
     * DS method for removing the SipStack service reference.
     * 
     */
	public void unsetSipApplicationRouter(ServiceReference<SipApplicationRouter> sipApplicationRouterSvc) {
		s_sipApplicationRouterSvcRef.unsetReference(sipApplicationRouterSvc);
	}
	
	/**
     * Access to the SipApplicationRouter service
     * 
     */
	public static SipApplicationRouter getSipApplicationRouter() {
		return s_sipApplicationRouterSvcRef.getService();
	}

	/**
     * Activates SipApplicationRouter service reference
     * 
     */
	public static void activateSipApplicationRouter() {
		if(s_sipApplicationRouterSvcRef.getReference() == null) {
		  if (c_logger.isTraceDebugEnabled()) {
	            c_logger.traceDebug("activateSipApplicationRouter: no application router was found");
		  }
		  return;
		  
		}
		
		synchronized (SipContainerComponent.class) {
			if (s_initialized) {
				return;
			}
			s_sipApplicationRouterSvcRef.activate(m_context);
			SipApplicationRouter sipAppRouter = s_sipApplicationRouterSvcRef.getService();
	        sipAppRouter.init(PropertiesStore.getInstance().getProperties().copyProps());
	        sipAppRouter.init();
	        SipRouter sipRouter = SipContainer.getInstance().getRouter();
	        sipRouter.initialize( sipAppRouter);
	        sipRouter.notifyRouterOnDeployedApps();
	        
	        s_initialized = true;
		}
	}
	
    /**
     * DS method for setting the Domain resolver service reference.
     * 
     * @param DomainResolverImpl
     */
	@Reference(service=DomainResolverImpl.class, policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
    public void setDomainResolverService(DomainResolverImpl domainResolverSvc) { 

		_domainResolver = domainResolverSvc;
		//SipStackDomainResolverImpl.setInstance(_domainResolver);
    }
	

	/**
     * DS method for removing the Domain resolver service reference.
     * 
     * @param DomainResolverImpl
     */
    public static DomainResolverImpl getDomainResolverService() { 
    	return _domainResolver;
    }
	
	/**
     * DS method for removing the Domain resolver service reference.
     * 
     * @param DomainResolverImpl
     */
    public void unsetDomainResolverService(DomainResolverImpl domainResolverSvc) { 
    	
    	_domainResolver = null;
    
    }


}
