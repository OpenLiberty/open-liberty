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
package com.ibm.ws.sip.stack.util;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.pmi.PerformanceMgr;

public class StackTaskDurationMeasurer {
	private long _startTime = 0;
	private long _totalTime = 0;
	
	/**
     * Class Logger.
     */
    private static final LogMgr c_logger = Log.get(StackTaskDurationMeasurer.class);
    
    public void startMeasuring() {
    	if(_startTime != 0) {
    		if(c_logger.isTraceDebugEnabled()) {
    			c_logger.traceDebug(this,"startMeasuring",
						"TaskDurationMeasurer was already initialized before, initializing again");
    		}
    	}
    	_startTime = PerformanceMgr.getInstance().getCurrentCachedTime();
    	
    }
    
    public long takeTimeMeasurement() {
    	long currentTime = PerformanceMgr.getInstance().getCurrentCachedTime();
    	_totalTime = currentTime - _startTime;
    	if(c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug(this,"takeTimeMeasurement",
        							" meassured task duration = " + _totalTime);
        }   
    	
    	return _totalTime;
    }
}
