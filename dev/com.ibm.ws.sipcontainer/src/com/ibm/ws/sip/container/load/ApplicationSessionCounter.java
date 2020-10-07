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

/**
 * @author anat
 * Represents the number of concurrent active SipApplicationSessions in 
 * the container. When new Application Session created appSessionCreated() 
 * method called and can influence the container's weight immediately. 
 * The same behavior when SipApplicationSession destroyed or invalidated.  
 */
public class ApplicationSessionCounter extends LoadCounterAbs {
	
	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(ApplicationSessionCounter.class);/**
	 * Represents the number of concurrent SipApplicationSessions in the container.
	 */
	private int _counter = 0;
	
	/**
	 * Reference to ConcurentLoadListener;
	 */
	private ConcurentLoadListener _lstr = null;
	
	/**
	 * Ctor
	 * @param maximumAllowed
	 */
	public ApplicationSessionCounter(	int maximumAllowed, 
										int waterMarkSize, 
										int initialWeight,
										ConcurentLoadListener lstr) {
			super(maximumAllowed,waterMarkSize,initialWeight,PerfUtil.APP_WEIGHT_INT);
			_lstr = lstr;
	}

	/**
	 * @see com.ibm.ws.sip.container.load.Weighable#increment()
	 *
	 * When new SipApplicationSession created - ApplicationSessionCounter
	 * will be notified about it.
	 */
	public void increment() {
		_counter ++;
		updateNewWeight();
	}

	/**
	 * @see com.ibm.ws.sip.container.load.Weighable#decrement()
	 * When new SipApplicationSession invalidated - ApplicationSessionCounter
	 * will be notified about it.
	 */
	public void decrement() {
		_counter--;
		updateNewWeight();
	}
	
    /**
     * Helper method that calculates new weight and updates the LoadMgr.
     */
	private void updateNewWeight(){
		long counter = _counter;
		if(calculateNewWeight(counter)){
			if (c_logger.isTraceDebugEnabled()) {
				c_logger.traceDebug(this, "updateNewWeight", 
						"Try to set new weight = " + getWeight());
			}
			_lstr.setNewWeight(this,counter);
		}
    }
	
	/** 
	 * @see com.ibm.ws.sip.container.load.LoadCounterAbs#getCurrentLoad()
	 */
	public long getCurrentLoad() {
		return _counter;
	}

	/** 
	 * @see com.ibm.ws.sip.container.load.LoadCounterAbs#reset()
	 */
	void reset() {
		// Do nothing.
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
		buff.append(_counter);
		return buff.toString();
	}
}
