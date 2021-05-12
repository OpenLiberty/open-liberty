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

import com.ibm.ws.sip.container.pmi.TaskDurationModuleInterface;
import com.ibm.ws.sip.container.pmi.taskduration.TaskDurationCounter;

public class TaskDurationModule implements TaskDurationModuleInterface{
    
    private TaskDurationCounter _processingQueueTaskDurationCounter = new TaskDurationCounter();    
    private TaskDurationCounter _outboundTaskDurationCounter = new TaskDurationCounter();
       
   
    /**
     * CTOR
     */
    public TaskDurationModule() {      
    }
    
    /**
     * Application destroyed, unregister PMI module associated with this
     * application.
     */
    public void destroy() {      
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.TaskDurationModuleInterface#updateTaskDurationProcessingQueueStatistics(long)
     */
    public synchronized void updateTaskDurationProcessingQueueStatistics(long ms) {
    	_processingQueueTaskDurationCounter.updateTaskDurationStatistics(ms);
//        if (s_logger.isLoggable(Level.FINEST)) {
//            StringBuilder buf = new StringBuilder();
//            buf.append("New task duration in Processing queue = " + ms);
//            s_logger.logp(Level.FINEST, TaskDurationModule.class.getName(),
//					"updateTaskDurationProcessingQueueStatistics", buf.toString());
//        }
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.TaskDurationModuleInterface#updateTaskDurationOutboundQueueStatistics(long)
     */
    public synchronized void updateTaskDurationOutboundQueueStatistics(long ms) {
    	_outboundTaskDurationCounter.updateTaskDurationStatistics(ms);
//        if (s_logger.isLoggable(Level.FINEST)) {
//            StringBuilder buf = new StringBuilder();
//            buf.append("New task duration in outbound queue = " + ms);
//            s_logger.logp(Level.FINEST, TaskDurationModule.class.getName(),
//					"updateTaskDurationOutboundQueueStatistics", buf.toString());
//        }
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.TaskDurationModuleInterface#updatePMICounters()
     */
    public synchronized void updatePMICounters() {
    	TaskDurationCounters.getInstance().setAvgTaskDurationInProcessingQueue(
    			_processingQueueTaskDurationCounter.getAvgTaskDurationOut());
    	TaskDurationCounters.getInstance().setMaxTaskDurationInProcessingQueue(
    			_processingQueueTaskDurationCounter.getMaxTaskDurationOut());
    	TaskDurationCounters.getInstance().setMinTaskDurationInProcessingQueue(
    			_processingQueueTaskDurationCounter.getMinTaskDurationOut());
    	TaskDurationCounters.getInstance().setAvgTaskDurationOutBoundQueue(
    			_outboundTaskDurationCounter.getAvgTaskDurationOut());
    	TaskDurationCounters.getInstance().setMaxTaskDurationOutBoundQueue(
    			_outboundTaskDurationCounter.getMaxTaskDurationOut());
    	TaskDurationCounters.getInstance().setMinTaskDurationOutBoundQueue(
    			_outboundTaskDurationCounter.getMinTaskDurationOut());
    	
    	_processingQueueTaskDurationCounter.init();
    	_outboundTaskDurationCounter.init();
    }

    /**
     * @see com.ibm.ws.sip.container.pmi.TaskDurationModuleInterface#isProcessingQueuePMIEnabled()
     */
	public boolean isProcessingQueuePMIEnabled() {
		return TaskDurationCounters.getInstance().isTaskDurationMonitoringEnabled();
	}

	/**
     * @see com.ibm.ws.sip.container.pmi.TaskDurationModuleInterface#isOutboundQueuePMIEnabled()
     */
	public boolean isOutboundQueuePMIEnabled() {
		return TaskDurationCounters.getInstance().isTaskDurationMonitoringEnabled();
	}
}
