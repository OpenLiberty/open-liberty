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
package com.ibm.ws.sip.container.was.message;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.sip.container.parser.SipAppDesc;

/**
 * A factory to generate sip messages.
 * This factory enables us to replace the sip message being created according to the factory type and to allow us to replace SipMessage to SipMessage31 if there is the servlets31 feature is enabled.
 * @author sagia
 *
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE, configurationPid = "com.ibm.ws.sip.container.was.message.SipMessageFactory", service=SipMessageFactory.class,
property = {"service.vendor=IBM"} )
public class SipMessageFactory {
	
	
	/**
     * DS method to activate this component.
     * 
     * @param properties : Map containing service & config properties
     *            populated/provided by config admin
     */
    protected void activate(ComponentContext context) {
    	
    }

    /**
     * DS method to deactivate this component.
     * 
     * @param reason int representation of reason the component is stopping
     */
    public void deactivate(int reason) {
    	

    }
    
    
    /**
     * returns a new sip message that is ready to be used.
     */
    public SipMessage createSipMessage(SipServletRequest request, SipServletResponse response,SipAppDesc appDesc){
    	SipMessage msg = createInternalMessage();
    	msg.setup(request, response, appDesc);
    	return msg;
    }

	/**
	 * create the sip message
	 * @return new sip message - without setup not ready to be used
	 */
	protected SipMessage createInternalMessage() {
		
		return new SipMessage();
	}

}
