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
package com.ibm.ws.sip.monitor.mxbeans;


/**
 * MXBean interface.Any new counter need to have a getter method over here in order to make the counter available as Attribute for the MXBean.
 */
public interface SipContainerBasicCountersMXBean {
	
	/** Gets a number of concurrently active Sip Application Sessions */
    public long getSipAppSessions();

    /** Gets a number of concurrently active Sip Sessions */
    public long getSipSessions();
    
    /**
     * Gets a number of SIP messages received by the Connector during the
     * PerformanceMgr.TRAFFIC_TIME_PERIOD_DEFAULT
     */
    public long getReceivedSipMsgs();

    /**
     * Gets a number of new Sip Application sessions created in Container during the
     * PerformanceMgr.TRAFFIC_TIME_PERIOD_DEFAULT
     */
    public long getNewSipApplications();
    
    /**
     * Gets time processing of SIP Request during the
     * PerformanceMgr.TRAFFIC_TIME_PERIOD_DEFAULT
     */
    public long getSipRequestProcessing();
    
//    /**
//     * Gets a number of replicated SIP Sessions
//     */
//    public Gauge getReplicatedSipSessions();
//    
//    /**
//     * Gets a number of not replicated SIP Sessions
//     */
//    public Gauge getNotReplicatedSipSessions();
//    
//    /**
//     * Gets a number of replicated SIP Application Sessions
//     */
//    public Gauge getReplicatedSipAppSessions();
//    
//    /**
//     * Gets a number of not replicated SIP Application Sessions
//     */
//    public Gauge getNotReplicatedSipAppSessions();
    
    /**
     * Gets a number of rejected messages
     */
    public long getRejectedMessages();
    
    /**
     * Gets a number of SIP timers invocations
     */
    public long getSipTimersInvocations();

    /** 
     * Gets the average the invoker queue size 
     */
    public long getInvokerSize();
    
}
