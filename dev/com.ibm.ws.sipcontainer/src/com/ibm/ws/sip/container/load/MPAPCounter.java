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
 * Represents Messages Per Average Period counter. 
 * Each received message will increase the _numberOfMsg and every 1 sec 
 * (relay on LoadTimer) _counter will be updated by _ numberOfMsg. 
 * _counter will be created with minimum 10 cells (each cell represents 1 sec) 
 * but if "average period (in sec)" will be higher than 10 - _couner will be 
 * created with number of cells equal to "average period (in sec)".
 *
 */
public class MPAPCounter extends LoadCounterAbs {
	
	/**
	 * Represent CounterArray of collected information about amount of
	 * incoming messages.
	 */
	private CounterArray _counter;

	/**
	 * Counter for the incoming messages received in the last sec. 
	 * Once a sec when timer executed this parameter will be moved to
	 * _counter.
	 */
	private int _numberOfMsg;

	
	/**
	 * Ctor
	 * @param maximumAllowed
	 */
	public MPAPCounter(	int maximumAllowed,
						int waterMarkSize, 
						int initialWeight,
						int averagePeriod) {
		super(maximumAllowed,
				waterMarkSize,
				initialWeight,
				PerfUtil.MPAP_WEIGHT_INT,
				averagePeriod);
		_counter = new CounterArray(averagePeriod);
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#increment()
	 * 
	 * Call when new incoming message received.
	 */
	public void increment() {
		_numberOfMsg++;
	}

	/**
	 * @see com.ibm.ws.sip.container.load.LoadCounterAbs#getCurrentLoad()
	 */
	public long getCurrentLoad() {
		_counter.addCounter(_numberOfMsg);
		return _counter.getAverage();
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.LoadCounterAbs#reset()
	 */
	void reset() {
		// This couner shoudl be nullified after each timer execution.
		_numberOfMsg = 0;
	}

	/**
	 * @see com.ibm.ws.sip.container.load.Weighable#decrement()
	 */
	public void decrement() {
		// Not implemented here
	}

	/**
	 * @see com.ibm.ws.sip.container.load.Weighable#setCounter(long)
	 */
	public void setCounter(long counter) {
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
		buff.append(" \n\r NumberOfMsg = ");
		buff.append(_numberOfMsg);
		
		return buff.toString();
	}
}
