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

public interface TaskDurationModuleInterface {
	//  Constants
    public final static int AVG_TASK_DURATION_PROC = 21;
    public final static int MAX_TASK_DURATION_PROC = 22;
    public final static int MIN_TASK_DURATION_PROC = 23;
    public final static int AVG_TASK_DURATION_OUT = 24;
    public final static int MAX_TASK_DURATION_OUT = 25;
    public final static int MIN_TASK_DURATION_OUT = 26;
        
    /**
     * update task duration in outbound queue
     *  
     */
    public void updateTaskDurationProcessingQueueStatistics(long ms);
    
    /**
     * update task duration in inbound queue (SIP stack queue)
     *  
     */
    public void updateTaskDurationOutboundQueueStatistics(long ms);
    
    /**
     * Update counters that were countered till now
     *
     */
    public void updatePMICounters();
    
    /**
     * Unregister module 
     */
    public void destroy();
    
    /**
     * Is processing queue monitoring enabled
     */
    public boolean isProcessingQueuePMIEnabled();
    
    /**
     * Is outbound queue monitoring enabled
     */
    public boolean isOutboundQueuePMIEnabled();
}
