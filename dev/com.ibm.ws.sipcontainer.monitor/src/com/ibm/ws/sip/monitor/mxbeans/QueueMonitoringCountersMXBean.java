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


public interface QueueMonitoringCountersMXBean {
	
	 /** Total number of tasks that have flowed through the processing SIP queue */
    public long getTotalTasksCountInProcessingQueue();
    
    /** Peak number of tasks in the processing SIP container queue */
    public long getPeakTasksCountInProcessingQueue();
    
    /** Minimum number of tasks in the processing SIP container queue */
    public long getMinTasksCountInProcessingQueue();
    
    /** Peak percent full of the processing SIP queue */
    public long getPercentageFullTasksCountInProcessingQueue();
    
    /** Total number of tasks that have flowed through the outbound SIP stack queue */
    public long getTotalTasksCountInOutboundQueue();
    
    /** Peak number of tasks in the outbound SIP stack queue */
    public long getPeakTasksCountInOutboundQueue();
    
    /** Minimum number of tasks in the outbound SIP stack queue */
    public long getMinTasksCountInOutboundQueue();
    
    /** Peak percent full of the outbound SIP stack queue */
    public long getPercentageFullTasksCountInOutboundQueue();

}
