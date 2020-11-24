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
package com.ibm.ws.sip.monitor.pmi.inbound;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;

/**
 * Monitor Class for the sip container inbound responses.
 */
@Monitor(group = "SipContainerInboundResponse")
public class InboundResponseCountersMonitor {
	
	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(InboundResponseCountersMonitor.class
		.getName());

	private final String _name = "SipContainer";
	
	@PublishedMetric
	public MeterCollection<InboundResponseCounters> sipCountByName = new MeterCollection<InboundResponseCounters>("SipContainer", this);

	public InboundResponseCountersMonitor() {	
		if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
    		s_logger.logp(Level.FINEST, InboundResponseCountersMonitor.class.getName(), 
    				"InboundResponseCountersMonitor", "creating a new InboundResponseCounters");
    	}
    	String _key = _name + ".InboundResponse";
    	InboundResponseCounters nStats = InboundResponseCounters.getInstance();
        this.sipCountByName.put(_key, nStats); 
    }
}
