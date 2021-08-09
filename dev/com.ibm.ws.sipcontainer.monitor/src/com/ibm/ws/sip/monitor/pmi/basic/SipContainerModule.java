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
package com.ibm.ws.sip.monitor.pmi.basic;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.pmi.server.PmiRegistry;
import com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener;
import com.ibm.ws.sip.monitor.pmi.queuemonitor.QueueMonitoringModule;
import com.ibm.ws.sip.monitor.pmi.taskduration.TaskDurationModule;

@Component(configurationPolicy = ConfigurationPolicy.IGNORE)
public class SipContainerModule implements SipContainerPMIListener {
	
	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(SipContainerModule.class
		.getName());
	
    private TaskDurationModule _taskDurationModule = null;
    
	/**
	 * This is the name of the resource bundle for all loggers produced
	 * @see java.util.ResourceBundle for information about resource bundles  
	 */		
	public static final String BUNDLE_NAME = "com.ibm.ws.sip.monitor.resources.Messages";
    
    /**
     * Ctor
     * 
     * @param appName
     *            name of the represented application
     */
    public SipContainerModule() {
    	       
    }
    
    /**
     * Activate this service component.
     */
    @Activate
    protected void activate(Map<String, Object> componentConfig) { 
    	if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
    		s_logger.log(Level.FINEST, "SipContainerModule activated", componentConfig);
    	}
      _taskDurationModule = new TaskDurationModule();  
      
    }

    @Modified
	protected void modified(Map<String, Object> properties) {
    }
   
    /**
     * Deactivate this component.
     */
    @Deactivate
    protected void deactivate(int reason) {
    }
    
    /**
     * 
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateAppSessionNum(long)
     */
    public void updateAppSessionNum(long num)  {
    	SipContainerBasicCounters.getInstance().setSipAppSessions(num);
    }

    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateSipSessionNum(long)
     */
    public void updateSipSessionNum(long num) {
    	SipContainerBasicCounters.getInstance().setSipSessions(num);
    }
 
    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateInvokeCounter(long)
     */
    public void updateInvokeCounter(long size) {
    	SipContainerBasicCounters.getInstance().setInvokerSize(size);
    }

    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateReceivedMsgs(long)
     */
    public void updateReceivedMsgs(long num) {
    	SipContainerBasicCounters.getInstance().setReceivedSipMsgs(num);
    }

    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateNewSipAppCreated(long)
     */
    public void updateNewSipAppCreated(long num) {
    	SipContainerBasicCounters.getInstance().setNewSipApplications(num);
    }

    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#processRequest(long)
     */
    public void updatedProcessingRequest(long ms) {
    	SipContainerBasicCounters.getInstance().setRequestProcessing(ms);
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateReplicatedSipSessionsCounter(long)
     */
    public void updateReplicatedSipSessionsCounter(long replicatedSessions) {    	
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateNotReplicatedSipSessionsCounter(long)
     */
    public void updateNotReplicatedSipSessionsCounter(long notReplicatedSessions) {
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateReplicatedSipAppSessionsCounter(long)
     */
    public void updateReplicatedSipAppSessionsCounter(long replicatedAppSessions) {    	
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateNotReplicatedSipSessionsCounter(long)
     */
    public void updateNotReplicatedSipAppSessionsCounter(long notReplicatedAppSessions) {    	
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.SipContainerPerf#updateTaskDurationProcessingQueueStatistics(long)
     */
	public void updateTaskDurationProcessingQueueStatistics(long ms) {
    	if(_taskDurationModule != null) {
    		_taskDurationModule.updateTaskDurationProcessingQueueStatistics(ms);
    	}
	}
	
	/**
     * @see com.ibm.ws.sip.container.pmi.SipContainerPerf#updateTaskDurationOutboundQueueStatistics(long)
     */
	public void updateTaskDurationOutboundQueueStatistics(long ms) {
    	if(_taskDurationModule != null) {
    		_taskDurationModule.updateTaskDurationOutboundQueueStatistics(ms);
    	}
	}
    
    /**
     * @see com.ibm.ws.sip.container.pmi.SipContainerPerf#updateTaskDurationPMICounters()
     */
    public void updateTaskDurationPMICounters() {
    	if(_taskDurationModule != null) {
    		_taskDurationModule.updatePMICounters();
    	}
    }
    
    /**
     * @return TaskDurationModel
     */
    public TaskDurationModule getTaskDurationModule() {
    	return _taskDurationModule;
	}
    
//    /**
//     * @return QueueMonitoringModule
//     */
//    public QueueMonitoringModule getQueueMonitoringModule() {
//    	return _queueMonitoringModule;
//    }
//    
	/**
	 * @see com.ibm.ws.sip.container.pmi.SipContainerPerf#updateQueueMonitoringTaskQueuedInProcessingQueue()
	 */
	public void updateQueueMonitoringTaskQueuedInProcessingQueue() {
		QueueMonitoringModule.getInstance().updateQueueMonitoringTaskQueuedInProcessingQueue();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.pmi.SipContainerPerf#updateQueueMonitoringTaskDequeuedFromProcessingQueue()
	 */
	public void updateQueueMonitoringTaskDequeuedFromProcessingQueue() {
		QueueMonitoringModule.getInstance().updateQueueMonitoringTaskDequeuedFromProcessingQueue();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.pmi.SipContainerPerf#updateQueueMonitoringTaskQueuedInOutboundQueue()
	 */
	public void updateQueueMonitoringTaskQueuedInOutboundQueue() {
		QueueMonitoringModule.getInstance().updateQueueMonitoringTaskQueuedInOutboundQueue();
	}
    
	/**
	 * @see com.ibm.ws.sip.container.pmi.SipContainerPerf#updateQueueMonitoringTaskDequeuedFromOutboundQueue()
	 */
	public void updateQueueMonitoringTaskDequeuedFromOutboundQueue() {
		QueueMonitoringModule.getInstance().updateQueueMonitoringTaskDequeuedFromOutboundQueue();
	}    

	/**
	 * @see com.ibm.ws.sip.container.pmi.SipContainerPerf#updateQueueMonitoringPMICounters()
	 */
	public void updateQueueMonitoringPMICounters() {
		QueueMonitoringModule.getInstance().updateQueueMonitoringPMICounters();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateRejectedMessagesCounter(long)
	 */
	public void updateRejectedMessagesCounter(long rejectedMessages) {
		SipContainerBasicCounters.getInstance().setRejectedSipMessages(rejectedMessages);
	}
	
	/**
	 * @see com.ibm.ws.sip.container.pmi.listener.SipContainerPMIListener#updateSipTimersInvocationsCounter(long)
	 */
	public void updateSipTimersInvocationsCounter(long timersInvocations) {
		SipContainerBasicCounters.getInstance().setSipTimersInvocations(timersInvocations);
	}

	@Override
	public boolean isTraditionalPMIEnabled() {
		return (!PmiRegistry.isDisabled());
	}

	@Override
	public boolean isProcessingQueuePMIEnabled() {
		return _taskDurationModule.isProcessingQueuePMIEnabled();
	}

	@Override
	public boolean isTaskDurationOutboundQueuePMIEnabled() {
		return _taskDurationModule.isOutboundQueuePMIEnabled();
	}

	@Override
	public boolean isQueueMonitoringProcessingQueuePMIEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isQueueMonitoringOutboundQueuePMIEnabled() {
		// TODO Auto-generated method stub
		return false;
	}
}
