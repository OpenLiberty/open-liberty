/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.was.servlet31;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import com.ibm.ws.sip.container.was.message.SipMessage;
import com.ibm.ws.sip.container.was.message.SipMessageFactory;

/**
 * Service that generate servlets31 sip messages.
 * The service is used in the case that servlets31 feature is enabled in addition to the sip container.
 * It will replace the SipMessageFactory and will use the servlets31 message.
 */
@Component(configurationPolicy = ConfigurationPolicy.IGNORE,
service = com.ibm.ws.sip.container.was.message.SipMessageFactory.class,
configurationPid = "com.ibm.ws.sip.container.was.servlet31.SipMessageFactory",
property = {"service.vendor=IBM","service.ranking:Integer=1" } )
public class SipMessageFactory31 extends SipMessageFactory {
	
	
	
	/**
     * DS method to activate this component.
     * 
     * @param 
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
	 * return a new SipMessage31 without setup and not ready for use.
	 * @see com.ibm.ws.sip.container.was.message.SipMessageFactory#createInternalMessage()
	 */
	protected SipMessage createInternalMessage() {
		
		return new SipMessage31();
	}
	
	

}
