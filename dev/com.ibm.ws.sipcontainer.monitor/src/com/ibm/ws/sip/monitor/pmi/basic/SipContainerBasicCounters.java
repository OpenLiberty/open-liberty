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

import java.util.logging.Logger;

import com.ibm.websphere.monitor.meters.Gauge;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.ws.sip.monitor.mxbeans.SipContainerBasicCountersMXBean;

public class SipContainerBasicCounters extends Meter implements
		SipContainerBasicCountersMXBean {
	
	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(SipContainerBasicCounters.class
		.getName());
	

	/** Singleton - initialized on activate */
    private static SipContainerBasicCounters s_singleton = null;
   
        
    /** Number of concurrently active Sip Application Sessions */
    private Gauge _sipAppSessionCounter = new Gauge();

    /** Number of concurrently active Sip Sessions */
    private Gauge _sipSessionCounter = new Gauge();
    
    /**
     * Number of SIP messages received by the Connector during the
     * PerformanceMgr.TRAFFIC_TIME_PERIOD_DEFAULT
     */
    private Gauge _receivedSipMsgsCounter = new Gauge();

    /**
     * Num of new Sip Application sessions created in Container during the
     * PerformanceMgr.TRAFFIC_TIME_PERIOD_DEFAULT
     */
    private Gauge _newSipApplicationCreated = new Gauge();
    
    /**
     * Time processing of SIP Request during the
     * PerformanceMgr.TRAFFIC_TIME_PERIOD_DEFAULT
     */
    private Gauge _sipRequestProcessing = new Gauge();    
    
    /**
     * Number of rejected messages
     */
    private Gauge _rejectedMessagesCounter = new Gauge();
    
    /**
     * Number of SIP timers invocations
     */
    private Gauge _sipTimersInvokations = new Gauge();

    /** Average the invoker queue size */
    private Gauge _invokerSize = new Gauge();
    
    /**
     * Ctor
     * 
     * @param appName
     *            name of the represented application
     */
    public SipContainerBasicCounters() {  
    	setDescription("This module provides the basic SIP container counters");
        
        _sipAppSessionCounter.setDescription("Sip Application Sessions counter that count SipApplicationSessions");
    	_sipSessionCounter.setDescription("Sip Sessions counter that count SipSessions");
        _receivedSipMsgsCounter.setDescription("Number of SIP messages received by the SIP Container during 10 secs or the predefined <pmiUpdateRange>");
        _newSipApplicationCreated.setDescription("Num of new Sip Application Sessions created in the SIP Container during 10 secs or the predefined <pmiUpdateRange>");
        _sipRequestProcessing.setDescription("Time processing of SIP Request during 10 secs or the predefined <pmiUpdateRange>");
        _rejectedMessagesCounter.setDescription("Number of rejected messages");
        _sipTimersInvokations.setDescription("Number of SIP timers invocations");
        _invokerSize.setDescription("Average the invoker queue size");
    }    
	
	@Override
	public long getSipAppSessions() {		
		return _sipAppSessionCounter.getCurrentValue();
	}

	@Override
	public long getSipSessions() {
		return _sipSessionCounter.getCurrentValue();
	}

	@Override
	public long getReceivedSipMsgs() {		
		return _receivedSipMsgsCounter.getCurrentValue();
	}

	@Override
	public long getNewSipApplications() {
		return _newSipApplicationCreated.getCurrentValue();
	}

	@Override
	public long getSipRequestProcessing() {
		return _sipRequestProcessing.getCurrentValue();
	}

	@Override
	public long getRejectedMessages() {
		return _rejectedMessagesCounter.getCurrentValue();
	}

	@Override
	public long getSipTimersInvocations() {
		return _sipTimersInvokations.getCurrentValue();
	}

	@Override
	public long getInvokerSize() {
		return _invokerSize.getCurrentValue();
	}
	
	public void setInvokerSize(long size) {
		_invokerSize.setCurrentValue(size);
	}

	public void setReceivedSipMsgs(long num) {
		_receivedSipMsgsCounter.setCurrentValue(num);
	}
	
	public void setNewSipApplications(long num) {
		_newSipApplicationCreated.setCurrentValue(num);
	}
	
	public void setRejectedSipMessages(long num) {
		_rejectedMessagesCounter.setCurrentValue(num);
	}
	
	public void setRequestProcessing(long ms) {
		_sipRequestProcessing.setCurrentValue(ms);
	}
	
	public void setSipAppSessions(long num) {
		_sipAppSessionCounter.setCurrentValue(num);
	}
	
	public void setSipSessions(long num) {
		_sipSessionCounter.setCurrentValue(num);
	}
	
	public void setSipTimersInvocations(long num) {
		_sipTimersInvokations.setCurrentValue(num);
	}
	
	public static SipContainerBasicCounters getInstance() {
		if (s_singleton == null)
			s_singleton = new SipContainerBasicCounters();		
		return s_singleton;
	}
}
