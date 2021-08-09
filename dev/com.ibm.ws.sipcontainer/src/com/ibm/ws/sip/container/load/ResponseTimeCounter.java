/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.container.load;

import com.ibm.ws.sip.container.pmi.PerfUtil;

/**
 * @author anat
 *
 * Represents maximum time from the moment when request received by the 
 * container and until the moment response sent by the application. 
 * _maxResponse  holds the last maximum response value in the current sec. 
 * Every 1 sec (relay on LoadTimer)  ResponseTimeCounter will update _counter 
 * array about new max response time during this last sec. _counter will be 
 * created by default with 10 cells when each cell represents 1 sec. 
 * getCurrentLoad() will return average of those cells.
 */
public class ResponseTimeCounter extends LoadCounterAbs{
	
	/**
	 * Represent CounterArray of collected information about maximum response
	 * time in each second
	 */
	private CounterArray _counter;

	/**
	 * Holds the maximum response time in the current second. When timer
	 * will be executed this information will be moved to _counter.
	 */
	private long _maxResponseTime;

	/**
	 * Ctor
	 * @param maximumAllowed
	 * @param averagePeriod
	 */
	public ResponseTimeCounter(	int maximumAllowed,
								int waterMarkSize, 
								int initialWeight,
								int averagePeriod) {
		super(maximumAllowed,
				waterMarkSize,
				initialWeight,
				PerfUtil.RESPONSE_WEIGHT_INT);
		_counter = new CounterArray(averagePeriod);
	}

	/**
	 * @see com.ibm.ws.sip.container.load.Weighable#setCounter(long)
	 * 
	 * Called by container each time when new response sent and response
	 * time can be calculated.
	 */
	public void setCounter(long counter){
		// Maximum response time will be always saved.
		// When getCurrentLoad() called (each timer execution = 1 sec)
		// _maxResponse will be nullified.
		if(counter > _maxResponseTime){
			_maxResponseTime = counter;
		}
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.LoadCounterAbs#getCurrentLoad()
	 */
	public long getCurrentLoad() {
		_counter.addCounter(_maxResponseTime);
		return _counter.getAverage();
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.LoadCounterAbs#reset()
	 */
	void reset() {
		// After each timer execution the _maxResponseTime field should be
		// nullified - _maxResponseTime is for message in 1 sec.
		_maxResponseTime = 0;
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#decrement()
	 */
	public void decrement() {
		// Not implemented here	
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#increment()
	 */
	public void increment() {
		// Not implemented here		
	}
	
	/*
	 * @see com.ibm.ws.sip.container.load.Weighable#getCurrentState()
	 */
	public String getCurrentState() {
		StringBuffer buff = new StringBuffer();
		buff.append(_myInfo);
		buff.append(" Counter = ");
		buff.append(_counter.getCurrentState());
		buff.append(" \n\r MaxResponseTime = ");
		buff.append(_maxResponseTime);
		
		return buff.toString();
	}
}
