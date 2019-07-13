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
package com.ibm.ws.sip.container.pmi;

public interface QueueMonitoringModuleInterface {

//  Constants
    public final static int TOTAL_TASKS_IN_PROC_QUEUE = 31;
    public final static int PEAK_TASKS_IN_PROC_QUEUE = 32;
    public final static int MIN_TASKS_IN_PROC_QUEUE = 33;
    public final static int PERCENT_FULL_TASKS_IN_PROC_QUEUE = 34;
    public final static int TOTAL_TASKS_IN_OUTBOUND_QUEUE = 35;
    public final static int PEAK_TASKS_IN_OUTBOUND_QUEUE = 36;
    public final static int MIN_TASKS_IN_OUTBOUND_QUEUE = 37;
    public final static int PERCENT_FULL_TASKS_IN_OUTBOUND_QUEUE = 38;
    
    /**
     * update queue monitoring statistics that task was queued in processing queue
     *  
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
    
    /**
     * Unregister module 
     */
    public void destroy();
    
    /**
     * Is processing queue monitoring enabled
     */
    public boolean isQueueMonitoringProcessingQueuePMIEnabled();
    
    /**
     * Is outbound queue monitoring enabled
     */
    public boolean isQueueMonitoringOutboundQueuePMIEnabled();
}
