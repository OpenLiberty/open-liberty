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

public class QueueMonitoringCounter {
	private int _totalTasksInQueue = 0;
	private int _peakTasks = 0;
	private int _minTasks = 0;
	private int _percentageFull = 0;
	private int _totalTasksArrivedDuringCurrentWindow = 0;
	protected  int _queueSize = 1000;
	
	public QueueMonitoringCounter() {
	}
	
	public synchronized void updateInTask() {
		_totalTasksInQueue++;
		_totalTasksArrivedDuringCurrentWindow++;
		updateStatistics();
	}
	
	public synchronized void updateOutTask() {
		_totalTasksInQueue--;
		updateStatistics();
	}
	
	private void updateStatistics() {
		if(_totalTasksInQueue > _peakTasks) {
			_peakTasks = _totalTasksInQueue;
			if(_queueSize > 0) {
				_percentageFull = (int)(100*((float)_peakTasks)/_queueSize);
			}
			else {
				_percentageFull = 0;
			}
		}
		
		if(_totalTasksInQueue < _minTasks) {
			_minTasks = _totalTasksInQueue;
		}
	}
	
	public synchronized void initStatistics() {
		_peakTasks = _totalTasksInQueue;
		_minTasks = _totalTasksInQueue;
		_totalTasksArrivedDuringCurrentWindow = 0;
		if(_queueSize > 0) {
			_percentageFull = (int)(100*((float)_peakTasks)/_queueSize);
		}
		else {
			_percentageFull = 0;
		}
	}
	
	public synchronized int getTotalTasksDuringCurrentWindow() {
		return _totalTasksArrivedDuringCurrentWindow;
	}
	
	public synchronized int getPeakTasks() {
		return _peakTasks;
	}
	
	public synchronized int getMinTasks() {
		return _minTasks;
	}
	
	public synchronized int getPercentageFull() {
		return _percentageFull;
	}
}
