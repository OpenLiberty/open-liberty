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
 * Performs the OperationTimeMeasurer in a multi-threaded environment
 * 
 * @author Nitzan Nissim
 */
public class ThreadSafeOperationTimeMeasurer extends OperationTimeMeasurer{
	private ThreadLocal _localMeasurer = new ThreadLocal();
	/**
	 * Ctor
	 * @param operationName
	 */
	public ThreadSafeOperationTimeMeasurer(String operationName){
		super(operationName);
    }
    
	/**
	 * Ctor
	 * @param operationName
	 * @param reportTimeInterval
	 */
    public ThreadSafeOperationTimeMeasurer(String operationName, long reportTimeInterval){
    	super(operationName, reportTimeInterval);
    }
    
    /**
     * Ctor
     * @param operationName
     * @param reportTimeInterval
     * @param reportPeriodically
     */
    public ThreadSafeOperationTimeMeasurer(String operationName, long reportTimeInterval, boolean reportPeriodically){
    	super(operationName, reportTimeInterval, reportPeriodically);
    }
    
    /**
     * @see com.ibm.ws.sip.container.util.OperationTimeMeasurer#start()
     */
	public void start(){
		if(!_enabled){
    		return;
    	}
		
		OperationTimeMeasurer measurer = (OperationTimeMeasurer)_localMeasurer.get();
		if( measurer == null){
			measurer = new OperationTimeMeasurer(_operationName, _reportTimeInterval, _reportPeriodically); 
			_localMeasurer.set(measurer);
		}
		measurer.start();
	}
	
	/**
	 * @see com.ibm.ws.sip.container.util.OperationTimeMeasurer#finish()
	 */
	public void finish(){
		if(!_enabled){
    		return;
    	}
		
		OperationTimeMeasurer measurer = (OperationTimeMeasurer)_localMeasurer.get();
		measurer.finish();
	}
}
