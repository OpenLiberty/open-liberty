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

public interface TaskDurationCountersMXBean {

	/** Avg task duration in sip container queue */
    public long getAvgTaskDurationInProcessingQueue();
    
    /** Maximum task duration in sip container queue*/
    public long getMaxTaskDurationInProcessingQueue();
    
    /** Minimum task duration in sip container queue*/
    public long getMinTaskDurationInProcessingQueue();
    
    /** Avg task duration in sip stack queue */
    public long getAvgTaskDurationOutBoundQueue();
    
    /** Maximum task duration in sip stack queue*/
    public long getMaxTaskDurationOutBoundQueue();
    
    /** Minimum task duration in sip stack queue*/
    public long getMinTaskDurationOutBoundQueue();
    
    /** Avg task duration in application */
    public long getAvgTaskDurationInApplication(String appName);
    
    /** Maximum task duration in application*/
    public long getMaxTaskDurationInApplication(String appName);
    
    /** Minimum task duration in application*/
    public long getMinTaskDurationInApplication(String appName);
}
