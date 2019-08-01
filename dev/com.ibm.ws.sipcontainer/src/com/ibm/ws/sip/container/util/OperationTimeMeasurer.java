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
package com.ibm.ws.sip.container.util;

/**
 * Helper class for debugging and performance assessment of code parts
 * 
 * @author Nitzan Nissim
 */
public class OperationTimeMeasurer{
    protected long _reportTimeInterval = 1000;
    protected String _operationName;
    protected boolean _reportPeriodically = false;
    protected boolean _enabled = false;
    
    private long _startTime = 0;
    private int _operationCount = 0;
    private long _totalTime = 0;
    private long _operationsIntervalStart = 0;
    
    /**
     * This flag will indicate that first report was already executed
     */
    private boolean _reportedOnce = false;
    
    /**
     * Ctor
     * @param operationName
     */
    public OperationTimeMeasurer(String operationName){
    	_operationName = operationName;
    }
    
    /**
     * Ctor
     * @param operationName
     * @param reportTimeInterval
     */
    public OperationTimeMeasurer(String operationName, long reportTimeInterval){
    	_operationName = operationName;
    	_reportTimeInterval = reportTimeInterval;
    }
    
    /**
     * Ctor
     * @param operationName
     * @param reportTimeInterval
     * @param reportPeriodically
     */
    public OperationTimeMeasurer(String operationName, long reportTimeInterval, boolean reportPeriodically){
    	_operationName = operationName;
    	_reportTimeInterval = reportTimeInterval;
    	_reportPeriodically = reportPeriodically;
    }
    
    /**
     * Enabling/disabling measurer  
     * @param enable
     */
    public void enable( boolean enable){
    	_enabled = enable;
    }
    
    /**
     * Call in the start of an operation
     * set start time.
     */
    public void start(){
    	if(!_enabled){
    		return;
    	}
    	
    	_startTime = System.currentTimeMillis();
    	if(_operationsIntervalStart == 0){
    		_operationsIntervalStart = _startTime;
    	}
    	_operationCount++;
    }
    
    /**
     * Call when operation is finished
     * Will measure operation duration and will report:
     * 1.Duration of first execution
     * 2. Average duration of operation when their total duration exceeds the _reportTimeInterval (default)
     * or 
     * 	  Average duration of operation when _reportTimeInterval has passed since last report 
     * 	  (if report periodically was set)
     */
    public void finish(){
    	if(!_enabled){
    		return;
    	}
    	
    	long currentTime = System.currentTimeMillis();
    	_totalTime+= currentTime - _startTime;
    	if( !_reportedOnce){//so that operations that were executed only once or several that took 
    						//less times then the report interval can be reported
    		report(_totalTime/_operationCount, _operationCount);
    		_reportedOnce = true;
    		return;
    	}
    	
    	if(_reportPeriodically){
    		if(currentTime - _operationsIntervalStart >= _reportTimeInterval){
    			report(_totalTime/_operationCount, _operationCount);
    			_operationsIntervalStart = 0;
    		}
    	}else
    	if(_totalTime >= _reportTimeInterval){
    		report(_totalTime/_operationCount, _operationCount);//reporting the average duration of operation within the 
    										//report interval
    		_operationCount = 0;
    		_totalTime = 0;
    	}
    }
    
    /**
     * Report the operation performance statistics
     * @param calcTime
     * @param count
     */
    protected void report( long calcTime, int count){
    	System.out.println("Operation " + _operationName + " duration measured " + calcTime 
    						+ " ms , operation executed"
    						+ ( !_reportedOnce ? " for the first time " : 
    											(_reportPeriodically ? 
    													" during " : " with total time of about " ) 
												+ _reportTimeInterval + " ms: count=" +_operationCount));
    }
}