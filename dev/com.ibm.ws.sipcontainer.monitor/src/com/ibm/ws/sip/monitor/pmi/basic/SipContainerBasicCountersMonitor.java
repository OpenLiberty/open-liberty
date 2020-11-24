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
package com.ibm.ws.sip.monitor.pmi.basic;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.websphere.monitor.meters.MeterCollection;

/**
 * Monitor Class for the sip container basic counters.
 * 
 * This class is responsible for calculating and reporting SIP basic counters provides by the <code>SipContainerBasicCountersMXBean</code>.
 */
@Monitor(group = "SipContainerBasic")
public class SipContainerBasicCountersMonitor {
	
	private final String _name = "SipContainer";
	
	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(SipContainerBasicCountersMonitor.class
		.getName());
	
	
	@PublishedMetric
	public MeterCollection<SipContainerBasicCounters> sipCountByName = new MeterCollection<SipContainerBasicCounters>(_name, this);

	/**
	 * Ctor 
	 * Acivated by monitor component if it's defined in the bundle as "Liberty-Monitoring-Components".
	 */
	public SipContainerBasicCountersMonitor() {			
		initSipContainerCounter();
	}
		
    private synchronized void initSipContainerCounter() {
        String _key = _name + "." + "Basic";
        Meter nStats = this.sipCountByName.get(_key);
        if (nStats == null) {      
        	if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
        		s_logger.logp(Level.FINEST, SipContainerBasicCountersMonitor.class.getName(), 
        				"initSipContainerCounter", "creating a new SipContainerBasicCounters");
        	}
            this.sipCountByName.put(_key, SipContainerBasicCounters.getInstance());       
        }
    }    
}
