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
package com.ibm.ws.sip.container.was;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.container.service.config.ServletConfigurator;
import com.ibm.ws.container.service.config.ServletConfiguratorHelper;
import com.ibm.ws.container.service.config.ServletConfiguratorHelperFactory;
import com.ibm.ws.sip.container.internal.SipContainerComponent;

/**
 * A declarative services component.
 * The component is responsible of processing Sip Annotations.
 * Is injected when a first application is deployed.
 * 
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
configurationPid = "com.ibm.ws.sip.container.was.sipServletConfigurator",
service = com.ibm.ws.container.service.config.ServletConfiguratorHelperFactory.class,
property = {"service.vendor=IBM"} )
public class SipServletConfiguratorHelperFactory implements ServletConfiguratorHelperFactory {
	
	/*
	 * Trace variable
	 */
	private static final TraceComponent tc = Tr.register(SipServletConfiguratorHelperFactory.class);

    @Override
    public ServletConfiguratorHelper createConfiguratorHelper(ServletConfigurator configurator) {
        return new SipServletConfiguratorHelper(configurator);
    }

    /**
	 * DS method to activate this component.
	 * 
	 * @param	context 	: Component context of the component 
	 * @param 	properties 	: Map containing service & config properties
	 *            populated/provided by config admin
	 */
    protected void activate(ComponentContext context, Map<String, Object> properties) {
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SipServletConfiguratorHelperFactory activated", properties);
    }

    /**
	 * DS method to deactivate this component.
	 * 
	 * @param reason int representation of reason the component is stopping
	 */
    public void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SipServletConfiguratorHelperFactory deactivated, reason="+reason);
        
    }
    
    /**
	 * DS method to modify components properties configuration
	 * 
	 * @param properties : Map containing service & config properties
	 *            populated/provided by config admin
	 */
    protected void modified(Map<String, Object> properties) {
    	if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "SipServletConfiguratorHelperFactory activated", properties);
    }
    
    /**
     * DS method for setting the SipContainerComponent service reference.
     * 
     * @param SipContainerComponent
     */
    @Reference(name="com.ibm.ws.sip.container.internal.SipContainerComponent", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY)
    public void setSipContainerComponent(SipContainerComponent sipContainerComponent) { 
    }
    
    /**
     * DS method for removing the Sip Application Router service reference.
     * 
     * @param SipContainerComponent
     */
    public void unsetSipContainerComponent(SipContainerComponent sipContainerComponent) { 
    }
}
