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

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.sip.container.pmi.PerfUtil;

public class DisabledCounter implements Weighable {
	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(DisabledCounter.class);

	/**
	 * Reference to ID of this object.
	 */
	int _myId;

	public DisabledCounter(int id) {
		if (c_logger.isTraceDebugEnabled()) {
			c_logger.traceDebug(this, "DisabledCounter",
					"This object created for " + PerfUtil.getTypeStr(id));
		}
		_myId = id;
	}
	
	/**
	 * @see com.ibm.ws.sip.container.load.Weighable#calculateWeight()
	 */
	public void calculateWeight() {

	}

	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#decrement()
	 */
	public void decrement() {
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#getCounterID()
	 */
	public int getCounterID() {
		return _myId;
	}

	/** 
	 * @see com.ibm.ws.sip.container.load.Weighable#getWeight()
	 */
	public int getWeight() {
		//Always return Maximum Weight !!!
		return LoadCounterAbs.FREE_WEIGHT;
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#increment()
	 */
	public void increment() {
		
	}

	/**
	 *  @see com.ibm.ws.sip.container.load.Weighable#setCounter(long)
	 */
	public void setCounter(long counter) {
		
	}

	public String getCurrentState() {
		return " DisabledCounter ";
	}

	public long getCurrentLoad() {
		return -1;
	}
	
	public long getLoadUsedForLastWeightCalc(){
		return 0;
	}

}
