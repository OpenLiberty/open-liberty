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

import com.ibm.websphere.monitor.meters.Gauge;
import com.ibm.websphere.monitor.meters.Meter;
import com.ibm.ws.sip.container.pmi.listener.ApplicationsPMIListener;
import com.ibm.ws.sip.monitor.mxbeans.TaskDurationCountersMXBean;
import com.ibm.ws.sip.monitor.pmi.application.ApplicationsModule;


public class TaskDurationCounters extends Meter implements
		TaskDurationCountersMXBean {

	/** Singleton - initialized on activate */
    private static TaskDurationCounters s_singleton = null;
    
    private boolean _isEnabled = false;
   

	/** Avg task duration in sip container queue */
    private Gauge _avgTaskDurationInProcessingQueue = new Gauge();
       
	/** Maximum task duration in sip container queue*/
    private Gauge _maxTaskDurationInProcessingQueue = new Gauge();
    
    /** Minimum task duration in sip container queue*/
    private Gauge _minTaskDurationInProcessingQueue = new Gauge();
    
    /** Avg task duration in sip stack queue */
    private Gauge _avgTaskDurationOutBoundQueue = new Gauge();
    
    /** Maximum task duration in sip stack queue*/
    private Gauge _maxTaskDurationOutBoundQueue = new Gauge();
    
    /** Minimum task duration in sip stack queue*/
    private Gauge _minTaskDurationOutBoundQueue = new Gauge();
    
	@Override
	public long getAvgTaskDurationInProcessingQueue() {		
		return _avgTaskDurationInProcessingQueue.getCurrentValue();
	}

	@Override
	public long getMaxTaskDurationInProcessingQueue() {
		return _maxTaskDurationInProcessingQueue.getCurrentValue();
	}

	@Override
	public long getMinTaskDurationInProcessingQueue() {
		return _maxTaskDurationInProcessingQueue.getCurrentValue();
	}

	@Override
	public long getAvgTaskDurationOutBoundQueue() {
		return _avgTaskDurationOutBoundQueue.getCurrentValue();
	}

	@Override
	public long getMaxTaskDurationOutBoundQueue() {
		return _maxTaskDurationOutBoundQueue.getCurrentValue();
	}

	@Override
	public long getMinTaskDurationOutBoundQueue() {
		return _minTaskDurationOutBoundQueue.getCurrentValue();
	}
	
	public static TaskDurationCounters getInstance() {
		if (s_singleton == null)
			s_singleton = new TaskDurationCounters();		
		return s_singleton;
	}
	
	 /**
	 * @param value the _avgTaskDurationInProcessingQueue to set
	 */
	public void setAvgTaskDurationInProcessingQueue(long value) {
		this._avgTaskDurationInProcessingQueue.setCurrentValue(value);
	}

	/**
	 * @param value the _maxTaskDurationInProcessingQueue to set
	 */
	public void setMaxTaskDurationInProcessingQueue(long value) {
		this._maxTaskDurationInProcessingQueue.setCurrentValue(value);
	}

	/**
	 * @param value the _minTaskDurationInProcessingQueue to set
	 */
	public void setMinTaskDurationInProcessingQueue(long value) {
		this._minTaskDurationInProcessingQueue.setCurrentValue(value);
	}

	/**
	 * @param value the _avgTaskDurationOutBoundQueue to set
	 */
	public void setAvgTaskDurationOutBoundQueue(long value) {
		_avgTaskDurationOutBoundQueue.setCurrentValue(value);
	}

	/**
	 * @param value the _maxTaskDurationOutBoundQueue to set
	 */
	public void setMaxTaskDurationOutBoundQueue(long value) {
		_maxTaskDurationOutBoundQueue.setCurrentValue(value);
	}

	/**
	 * @param value the _minTaskDurationOutBoundQueue to set
	 */
	public void setMinTaskDurationOutBoundQueue(long value) {
		_minTaskDurationOutBoundQueue.setCurrentValue(value);
	}
	
	@Override
	public long getAvgTaskDurationInApplication(String appName) {
		ApplicationsPMIListener module = ApplicationsModule.getInstance();
		if (module != null) {
			return module.getApplicationModule(appName).getApplicationTaskDurationModule().getAvgTaskDurationInApplication();
		}
		return 0;
	}

	@Override
	public long getMaxTaskDurationInApplication(String appName) {
		ApplicationsPMIListener module = ApplicationsModule.getInstance();
		if (module != null) {
			return module.getApplicationModule(appName).getApplicationTaskDurationModule().getMaxTaskDurationInApplication();
		}
		return 0;
	}

	@Override
	public long getMinTaskDurationInApplication(String appName) {
		ApplicationsPMIListener module = ApplicationsModule.getInstance();
		if (module != null) {
			return module.getApplicationModule(appName).getApplicationTaskDurationModule().getMinTaskDurationInApplication();
		}
		return 0;
	}
	
	 /**
	 * @return the _isEnabled
	 */
	public boolean isTaskDurationMonitoringEnabled() {
		return _isEnabled;
	}

	/**
	 * @param _isEnabled to set
	 */
	public void enableTaskDurationMonitoringEnabled(boolean isEnable) {
		_isEnabled = isEnable;
	}

}
