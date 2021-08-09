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
package com.ibm.ws.sip.container.pmi.taskduration;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

public class TaskDurationCounter {
		
	// members related to outbound sip container queue statistics
	private int _totalTasks = 0;
	private long _totalTime = 0;
	private long _avgTaskDuration = 0;
	private long _maxTaskDuration = 0;
	private long _minTaskDuration = 0;
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr s_logger = Log.get(TaskDurationCounter.class);
	
	//
	public synchronized void updateTaskDurationStatistics(long ms) {
		_totalTasks++;
		_totalTime += ms;
		
		//calculate maximum time duration of a task in the outbound queue
		if(_totalTasks == 1) {
			_maxTaskDuration = ms;
		}
		else {
			if(ms > _maxTaskDuration) {
				_maxTaskDuration = ms;
			}
		}
		
		//calculate minimum time duration of a task in the outbound queue
		if(_totalTasks == 1) {
			_minTaskDuration = ms;
		}
		else {
			if(ms < _minTaskDuration) {
				_minTaskDuration = ms;
			}
		}
		
		if (s_logger.isTraceDebugEnabled()) {
            s_logger.traceDebug(this, "updateTaskDurationStatistics", 
                                "task duration average = " + _avgTaskDuration
                                + " min task duration = " + _minTaskDuration
                                + " max task duration = " + _maxTaskDuration
                                + " total tasks = " + _totalTasks
                                + " total time = " + _totalTime);
        }
	}
	
	public synchronized long getAvgTaskDurationOut() {		
		if(_totalTime == 0 || _totalTasks == 0)
		{
			_avgTaskDuration = 0;
		}
		else {
			_avgTaskDuration =  (long)(((float)_totalTime)/_totalTasks);
		}
		
		if (s_logger.isTraceDebugEnabled()) {
            s_logger.traceDebug(this, "getAvgTaskDurationOut", 
                                "task duration average = " + _avgTaskDuration
                                + " total tasks = " + _totalTasks
                                + " total time = " + _totalTime);
        }
		
		return _avgTaskDuration;
	}
	
	public synchronized long getMaxTaskDurationOut() {
		
		if (s_logger.isTraceDebugEnabled()) {
            s_logger.traceDebug(this, "getMaxTaskDurationOut", 
                                "maximum task duration = " + _maxTaskDuration);
		}
		return _maxTaskDuration;
	}
	
	public synchronized long getMinTaskDurationOut() {
			
		if (s_logger.isTraceDebugEnabled()) {
            s_logger.traceDebug(this, "getMinTaskDurationOut", 
                                "minimum task duration = " + _minTaskDuration);
		}
		return _minTaskDuration;
	}
	
	/**
	 * Init counter values to zeros in the end of the update interval
	 */
	public synchronized void init() {
		_totalTasks = 0;
		_totalTime = 0;
		_avgTaskDuration = 0;
		_maxTaskDuration = 0;
		_minTaskDuration = 0;
	}
}
