/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.pmi.listener;

/**
 * This listener is used by the Sip Container monitor component to receive  
 * basic counters, task duration and queue monitoring updates.
 */
public interface SipContainerPMIListener
{   
	/**
	 * Indicates whether the monitor "enableTraditionalPMI" is enabled
	 * @return boolean
	 */
	public boolean isTraditionalPMIEnabled();
	
    /**
     * update num of SipApplicationSessions
     * @param num new num of application sessions
     */
    public void updateAppSessionNum(long num);
   
    /**
     * update num of SipSessions
     * @param num new num of application sessions
     */
    public void updateSipSessionNum(long num);    
    
    /**
     * Update traffic over connector
     * 
     * @param num -
     *            sum of the incomming and outqoing messaged during the last period
     */
    public void updateReceivedMsgs(long num);

    /**
     * counter that represent the number of the NEW sip application sessions
     * created during the counted period
     * @param num
     */
    public void updateNewSipAppCreated(long num);

    /**
     * Time period taken to process the request
     * 
     * @param msec =
     *            time <millisecond>
     */
    public void updatedProcessingRequest(long ms);

    /**
     * Update PMI with a new size of Invoke queue
     * @param size new size
     */
    public void updateInvokeCounter(long size);
    
    /**
     * Update task duration counter with a new task duration in processing queue statistic 
     * @param time duration
     */
    public void updateTaskDurationProcessingQueueStatistics(long ms);
    
    /**
     * Update task duration counter with a new task duration in outbound queue statistic 
     * @param time duration
     */
    public void updateTaskDurationOutboundQueueStatistics(long ms);
    
    /**
     * Update task duration PMI counters
     */
    public void updateTaskDurationPMICounters();    
    
    /**
     * update queue monitoring statistics that task was queued in outbound queue
     */
    public void updateQueueMonitoringTaskQueuedInProcessingQueue();
    
    /**
     * update queue monitoring statistics that task was dequeued out of processing queue
     *  
     */
    public void updateQueueMonitoringTaskDequeuedFromProcessingQueue();
    
    /**
     * update queue monitoring statistics that task was queued in outbound queue
     */
    public void updateQueueMonitoringTaskQueuedInOutboundQueue();
    
    /**
     * update queue monitoring statistics that task was dequeued out of outbound queue
     *  
     */
    public void updateQueueMonitoringTaskDequeuedFromOutboundQueue();
	
    /**
     * Initialize PMI counters with collected statistics
     *
     */
    public void updateQueueMonitoringPMICounters();
    
//    /**
//     * Update replicated Sip Sessions PMI counter
//     */
//    public void updateReplicatedSipSessionsCounter(long replicatedSessions);
//    
//    /**
//     * Update NOT replicated Sip Sessions PMI counter
//     */
//    public void updateNotReplicatedSipSessionsCounter(long notReplicatedSessions);
//    
//    /**
//     * Update replicated Sip App Sessions PMI counter
//     */
//    public void updateReplicatedSipAppSessionsCounter(long replicatedAppSessions);
//    
//    /**
//     * Update NOT replicated Sip App Sessions PMI counter
//     */
//    public void updateNotReplicatedSipAppSessionsCounter(long notReplicatedAppSessions);
//    
    /**
     * Update Rejected Messages PMI counter
     */
    public void updateRejectedMessagesCounter(long rejectedMessages);
    
    /**
     * Update SIP timers invocations PMI counter
     */
    public void updateSipTimersInvocationsCounter(long timersInvocations);
    
    /**
     * Is processing queue monitoring enabled
     */
    public boolean isProcessingQueuePMIEnabled();
    
    /**
     * Is outbound queue monitoring enabled
     */
    public boolean isTaskDurationOutboundQueuePMIEnabled();
    
    /**
     * Is processing queue monitoring enabled
     */
    public boolean isQueueMonitoringProcessingQueuePMIEnabled();
    
    /**
     * Is outbound queue monitoring enabled
     */
    public boolean isQueueMonitoringOutboundQueuePMIEnabled();
    
    
 }