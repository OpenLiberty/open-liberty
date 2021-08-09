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
package com.ibm.ws.sip.monitor.pmi.taskduration;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.monitor.annotation.Monitor;
import com.ibm.websphere.monitor.annotation.PublishedMetric;
import com.ibm.websphere.monitor.meters.MeterCollection;

/**
 * Monitor Class for the sip container task duration.
 */

@Monitor(group = "SipContainerTaskDuration")
public class TaskDurationCountersMonitor {
	
	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(TaskDurationCountersMonitor.class
		.getName());

	private final String _name = "SipContainer";
	
	@PublishedMetric
	public MeterCollection<TaskDurationCounters> sipCountByName = new MeterCollection<TaskDurationCounters>("SipContainer", this);

	public TaskDurationCountersMonitor() {	
		if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
    		s_logger.logp(Level.FINEST, TaskDurationCountersMonitor.class.getName(), 
    				"TaskDurationCountersMonitor", "creating a new TaskDurationCounters");
    	}
    	String _key = _name + ".TaskDuration";
    	TaskDurationCounters nStats = TaskDurationCounters.getInstance();
        this.sipCountByName.put(_key, nStats); 
        
        nStats.enableTaskDurationMonitoringEnabled(true);
    }
}
