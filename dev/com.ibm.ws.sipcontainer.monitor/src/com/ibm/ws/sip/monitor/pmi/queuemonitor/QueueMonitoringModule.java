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
package com.ibm.ws.sip.monitor.pmi.queuemonitor;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.monitor.meters.Gauge;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.ws.sip.container.pmi.OutboundQueueMonitoringCounter;
import com.ibm.ws.sip.container.pmi.QueueMonitoringCounter;
import com.ibm.ws.sip.container.pmi.QueueMonitoringModuleInterface;
import com.ibm.ws.sip.monitor.mxbeans.QueueMonitoringCountersMXBean;

public class QueueMonitoringModule extends Meter 
	implements QueueMonitoringCountersMXBean, QueueMonitoringModuleInterface {
	
	/**
	 * Class Logger. 
	 */
	private static final Logger s_logger = Logger.getLogger(QueueMonitoringModule.class
		.getName());

	
    /** Total number of tasks that have flowed through the processing SIP queue */
    private Gauge _totalTasksCountInProcessingQueue = new Gauge();
    
    /** Peak number of tasks in the processing SIP container queue */
    private Gauge _peakTasksCountInProcessingQueue = new Gauge();
    
    /** Minimum number of tasks in the processing SIP container queue */
    private Gauge _minTasksCountInProcessingQueue = new Gauge();
    
    /** Peak percent full of the processing SIP queue */
    private Gauge _percentageFullTasksCountInProcessingQueue = new Gauge();
    
    /** Total number of tasks that have flowed through the outbound SIP stack queue */
    private Gauge _totalTasksCountInOutboundQueue = new Gauge();
    
    /** Peak number of tasks in the outbound SIP stack queue */
    private Gauge _peakTasksCountInOutboundQueue = new Gauge();
    
    /** Minimum number of tasks in the outbound SIP stack queue */
    private Gauge _minTasksCountInOutboundQueue = new Gauge();
    
    /** Peak percent full of the outbound SIP stack queue */
    private Gauge _percentageFullTasksCountInOutboundQueue = new Gauge();
    
    private QueueMonitoringCounter _processingQueueMonitoringCounter;
    
    private QueueMonitoringCounter _outboundQueueMonitoringCounter;
    
    /** Singleton - initialized on activate */
    private static QueueMonitoringModule s_singleton = null;
    
    public static QueueMonitoringModule getInstance() {
		if (s_singleton == null)
			s_singleton = new QueueMonitoringModule();		
		return s_singleton;
	}
    
    /**
     * CTOR
     */
    public QueueMonitoringModule() {
        _processingQueueMonitoringCounter = new QueueMonitoringCounter();
        _outboundQueueMonitoringCounter = new OutboundQueueMonitoringCounter();        
    }
    
    /**
     * Application destroyed, unregister PMI module associated with this
     * application.
     *  
     */
    public void destroy() {
        
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.QueueMonitoringModuleInterface#updateQueueMonitoringTaskQueuedInProcessingQueue()
     */
    public synchronized void updateQueueMonitoringTaskQueuedInProcessingQueue() {
    	_processingQueueMonitoringCounter.updateInTask();
        if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
            s_logger.logp(Level.FINEST, QueueMonitoringModule.class.getName(),
					"updateQueueMonitoringTaskQueuedInProcessingQueue", "New task queued");
        }
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.QueueMonitoringModuleInterface#updateQueueMonitoringTaskDequeuedFromProcessingQueue()
     * 
     */
    public synchronized void updateQueueMonitoringTaskDequeuedFromProcessingQueue() {
    	_processingQueueMonitoringCounter.updateOutTask();
    	if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
            s_logger.logp(Level.FINEST, QueueMonitoringModule.class.getName(),
					"updateQueueMonitoringTaskDequeuedFromProcessingQueue", "Task dequeued");
        }
    };
    
    /**
     * @see com.ibm.ws.sip.container.pmi.QueueMonitoringModuleInterface#updateQueueMonitoringTaskQueuedInOutboundQueue()
     */
    public synchronized void updateQueueMonitoringTaskQueuedInOutboundQueue() {
    	_outboundQueueMonitoringCounter.updateInTask();
    	if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
            s_logger.logp(Level.FINEST, QueueMonitoringModule.class.getName(),
					"updateQueueMonitoringTaskDequeuedFromProcessingQueue", "New task queued");
        }
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.QueueMonitoringModuleInterface#updateQueueMonitoringTaskDequeuedFromOutboundQueue()
     */
    public synchronized void updateQueueMonitoringTaskDequeuedFromOutboundQueue() {
    	_outboundQueueMonitoringCounter.updateOutTask();
    	if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
            s_logger.logp(Level.FINEST, QueueMonitoringModule.class.getName(),
					"updateQueueMonitoringTaskDequeuedFromOutboundQueue", "Task dequeued");
        }
    }
    
    private void setPMICounterIfEnabled(Gauge counter, long value) {
    	counter.setCurrentValue(value);
    }
	
    /**
     * @see com.ibm.ws.sip.container.pmi.QueueMonitoringModuleInterface#updateQueueMonitoringPMICounters()
     */
    public synchronized void updateQueueMonitoringPMICounters() {
    	if (s_logger != null && s_logger.isLoggable(Level.FINEST)) {
            s_logger.logp(Level.FINEST, QueueMonitoringModule.class.getName(),
					"updateQueueMonitoringPMICounters", "");
        }
    	setPMICounterIfEnabled(_totalTasksCountInProcessingQueue, _processingQueueMonitoringCounter.getTotalTasksDuringCurrentWindow());
    	setPMICounterIfEnabled(_peakTasksCountInProcessingQueue, _processingQueueMonitoringCounter.getPeakTasks());	
    	setPMICounterIfEnabled(_minTasksCountInProcessingQueue, _processingQueueMonitoringCounter.getMinTasks());
    	setPMICounterIfEnabled(_percentageFullTasksCountInProcessingQueue, _processingQueueMonitoringCounter.getPercentageFull());
    	setPMICounterIfEnabled(_totalTasksCountInOutboundQueue, _outboundQueueMonitoringCounter.getTotalTasksDuringCurrentWindow());
    	setPMICounterIfEnabled(_peakTasksCountInOutboundQueue, _outboundQueueMonitoringCounter.getPeakTasks());
    	setPMICounterIfEnabled(_minTasksCountInOutboundQueue, _outboundQueueMonitoringCounter.getMinTasks());
    	setPMICounterIfEnabled(_percentageFullTasksCountInOutboundQueue, _outboundQueueMonitoringCounter.getPercentageFull());
    	
    	_processingQueueMonitoringCounter.initStatistics();
    	_outboundQueueMonitoringCounter.initStatistics();
    }
        
    /**
     * @see com.ibm.ws.sip.container.pmi.QueueMonitoringModuleInterface#isQueueMonitoringProcessingQueuePMIEnabled()
     */
    public synchronized boolean isQueueMonitoringProcessingQueuePMIEnabled() {
		return true;
    }
    
    /**
     * @see com.ibm.ws.sip.container.pmi.QueueMonitoringModuleInterface#isQueueMonitoringOutboundQueuePMIEnabled()
     */
    public boolean isQueueMonitoringOutboundQueuePMIEnabled() {
    	
		return true;
    }

	@Override
	public long getTotalTasksCountInProcessingQueue() {
		return _totalTasksCountInProcessingQueue.getCurrentValue();
	}

	@Override
	public long getPeakTasksCountInProcessingQueue() {
		return _peakTasksCountInProcessingQueue.getCurrentValue();
	}

	@Override
	public long getMinTasksCountInProcessingQueue() {
		return _minTasksCountInProcessingQueue.getCurrentValue();
	}

	@Override
	public long getPercentageFullTasksCountInProcessingQueue() {
		return _percentageFullTasksCountInProcessingQueue.getCurrentValue();
	}

	@Override
	public long getTotalTasksCountInOutboundQueue() {
		return _totalTasksCountInOutboundQueue.getCurrentValue();
	}

	@Override
	public long getPeakTasksCountInOutboundQueue() {
		return _peakTasksCountInOutboundQueue.getCurrentValue();
	}

	@Override
	public long getMinTasksCountInOutboundQueue() {
		return _minTasksCountInOutboundQueue.getCurrentValue();
	}

	@Override
	public long getPercentageFullTasksCountInOutboundQueue() {
		return _percentageFullTasksCountInOutboundQueue.getCurrentValue();
	}
}
