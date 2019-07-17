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
package com.ibm.ws.sip.stack.util;

public interface StackExternalizedPerformanceMgr {
	
	/**
     * Stack Task finished - take time measurements 
     */
	public void measureTaskDurationOutboundQueue(long ms);
	
	/**
	 * Get current time
	 */
	public long getCurrentCachedTime();
	
	/**
	 * is PMI enabled
	 */
	public boolean isPMIEnabled();
    
    /**
     * Is task duration monitoring for outbound queue enabled
     */
    public boolean isTaskDurationOutboundQueuePMIEnabled();
    
    /**
     * Is queue monitoring for outbound queue enabled
     */
    public boolean isQueueMonitoringOutboundQueuePMIEnabled();
    
    /**
     * New task queued - update queue monitoring statistics
     */
    public void updateQueueMonitoringTaskQueuedInOutboundQueue();
    
    /**
     * Task dequeued - update queue monitoring statistics
     */
    public void updateQueueMonitoringTaskDequeuedFromOutboundQueue();
    
    /**
     * Update Rejected Messages counter
     */
    public void updateRejectedMessagesCounter();
    
    /**
     * Update Sip timers invocations counter
     */
    public void updateSipTimersInvocationsCounter();
}
