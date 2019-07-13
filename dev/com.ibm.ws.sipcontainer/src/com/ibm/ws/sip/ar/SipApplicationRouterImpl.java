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
package com.ibm.ws.sip.ar;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.ar.SipApplicationRouter;
import javax.servlet.sip.ar.SipApplicationRouterInfo;
import javax.servlet.sip.ar.SipApplicationRoutingDirective;
import javax.servlet.sip.ar.SipApplicationRoutingRegion;
import javax.servlet.sip.ar.SipTargetedRequestInfo;
import javax.servlet.sip.ar.spi.SipApplicationRouterProvider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.properties.PropertiesStore;
import com.ibm.ws.sip.dar.DefaultApplicationRouter;
import com.ibm.ws.sip.properties.CoreProperties;
import com.ibm.ws.sip.properties.SipPropertiesMap;

/**
 * A declarative services component.
 * The component is responsible of Sip Application Router.
 * This components chooses which application router to create in it's activate method.
 * Is injected when a first application is deployed.
 */
@Component(service = SipApplicationRouter.class,
configurationPolicy = ConfigurationPolicy.OPTIONAL,
configurationPid = "com.ibm.ws.sip.ar",
name = "com.ibm.ws.sip.ar",
property = {"service.vendor=IBM"} )
public class SipApplicationRouterImpl implements SipApplicationRouter {
	
	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipApplicationRouterImpl.class);

	/*
	 * An instance of a SipApplicationRouter - can be DefaultApplicationRouter or CustomApplicationRouter
	 */
    private static SipApplicationRouter s_sipApplicationRouter = null;
    
	/**
	 * An instance of the selected car provider if it exists.
	 */
	private static SipApplicationRouterProvider s_sipApplicationRouterProvider;
    
	/**
	 * DS method to activate this component.
	 * 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 */
	protected void activate(Map<String, Object> properties) {
		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("SipApplicationRouterImpl activated", properties);
		SipPropertiesMap props = PropertiesStore.getInstance().getProperties();
        props.updateProperties(properties);
		initSipApplicationRouter();
	}
	
	/**
	 * DS method to deactivate this component.
	 * 
	 * @param reason int representation of reason the component is stopping
	 */
	public void deactivate(int reason) {
        if (c_logger.isTraceDebugEnabled())
            c_logger.traceDebug("SipApplicationRouterImpl deactivated, reason="+reason);
        
    }
	
    /**
	 * DS method to modify this component.
	 * 
	 * @param properties : Map containing service & config properties
	 *            populated/provided by config admin
	 */
    @Modified
	protected void modified(Map<String, Object> properties) {
		if (c_logger.isWarnEnabled()) {
			c_logger.warn("warn.dar.file.modification");
		}
	}
	
	/**
	 * implementation of choosing and initializing which application router to enable:
	 *  DefaultApplicationRouter or custom application router
	 */
	private void initSipApplicationRouter() {

		boolean enableCAR = PropertiesStore.getInstance().getProperties().getBoolean(CoreProperties.ENABLE_CAR);

		if((enableCAR == true) && (s_sipApplicationRouterProvider != null)) {
			s_sipApplicationRouter = s_sipApplicationRouterProvider.getSipApplicationRouter();
		}
		
		else {
			
			s_sipApplicationRouter = new DefaultApplicationRouter();
		}
	}
	/**
     * DS method for setting the  Custom application router provider if it exists
     */
	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
	protected void setSipApplicationRouterProvider(SipApplicationRouterProvider sarp) { 
		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("setSipApplicationRouterProvider ", sarp);
		s_sipApplicationRouterProvider = sarp;
	}
	
	
	

	/**
    * DS method for removing the Custom application router provider.
    * 
    */
	protected void unsetSipApplicationRouterProvider(SipApplicationRouterProvider sarp) {
		if (c_logger.isTraceDebugEnabled())
			c_logger.traceDebug("unsetSipApplicationRouterProvider ", sarp);
	}
	
	@Override
	public void applicationDeployed(List<String> newlyDeployedApplicationNames) {
		s_sipApplicationRouter.applicationDeployed(newlyDeployedApplicationNames);
	}

	@Override
	public void applicationUndeployed(List<String> undeployedApplicationNames) {
		s_sipApplicationRouter.applicationUndeployed(undeployedApplicationNames);
	}

	@Override
	public void destroy() {
		s_sipApplicationRouter.destroy();
	}

	@Override
	public SipApplicationRouterInfo getNextApplication(
			SipServletRequest initialRequest,
			SipApplicationRoutingRegion region,
			SipApplicationRoutingDirective directive,
			SipTargetedRequestInfo targetedRequestInfo, Serializable stateInfo)
					throws NullPointerException, IllegalStateException {
		
		return s_sipApplicationRouter.getNextApplication(initialRequest, region, directive, targetedRequestInfo, stateInfo);
	}

	@Override
	public void init() {
		s_sipApplicationRouter.init();
	}

	@Override
	public void init(Properties properties) throws IllegalStateException {
		s_sipApplicationRouter.init(properties);
	}

}
