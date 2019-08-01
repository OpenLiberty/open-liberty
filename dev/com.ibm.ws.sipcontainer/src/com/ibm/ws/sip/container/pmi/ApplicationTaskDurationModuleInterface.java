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

/**
 * This interface to manage SIP task duration counters per application.
 * 
 * @author galina
 *
 */
public interface ApplicationTaskDurationModuleInterface {
	
	//  Constants
    public final static int AVG_TASK_DURATION_APP = 911;
    public final static int MAX_TASK_DURATION_APP = 912;
    public final static int MIN_TASK_DURATION_APP = 913;
    
    /**
     * update task duration in application code
     *  
     */
    public void updateTaskDurationInApplication(long ms);
    
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
     * is task duration in application monitored
     */
    public boolean isApplicationDurationPMIEnabled();
    
    /** Avg task duration in application */
    public long getAvgTaskDurationInApplication();
    
    /** Maximum task duration in application*/
    public long getMaxTaskDurationInApplication();
    
    /** Minimum task duration in application*/
    public long getMinTaskDurationInApplication();
}
