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
import com.ibm.ws.sip.container.pmi.LoadManager;
import com.ibm.ws.sip.container.pmi.PerfUtil;

/**
 * 
 * LoadCounterAbs abstract class implementing Weighable interface that serves
 * as a base class for each counter that calculates weight in consideration 
 * of "low watermark". getCurrentLoad() is abstract method that should be 
 * implemented by each derived counter. Each counter calculates its own load, 
 * and LoadCounterAbs is responsible to calculate the current weight according 
 * to the load and _waterMarkSize.
 *
 * @author anat
 * @update moti change doc so mantis in linux does warn on UTF-8 characters
 *
 */
	public abstract class LoadCounterAbs implements Weighable {

	/**
	 * Class Logger.
	 */
	private static final LogMgr c_logger = Log.get(LoadCounterAbs.class);

	/**
	 * Defines the step size for each weight.
	 */
	private int _stepSize;

	/**
	 * Holds the last weight that was changed.
	 */
	private int _lastWeight = PerfUtil.INITIAL_WEIGHT; 
	
	/**
	 * Holds the maximum allowed defined in the WCCM.
	 */
	private int _maximumAllowed;

	/**
	 * Defines the size of low watermark . 
	 */
	private int _waterMarkSize;
	
	/**
	 * This counter identificator.
	 */
	private int _myId;

	/**
	 * Defines the weight value when container is fully free and available 
	 */
	public static final int FREE_WEIGHT = 10;
	
	/**
	 * String info about this counter.
	 */
	protected StringBuffer _myInfo = null;
	
	/**
	 * We need this value usually so that logging will be in sync with the load used for 
	 * weight calculation, so that same load will be printed and not recalculated
	 */
	private long _loadAtLastWeightCalc = 0;
	
	/**
	 * Ctor
	 * @param maximumAllowed
	 * @param waterMarkSize
	 * @param initialWeight
	 * @param id
	 * @param averagePeriod
	 */
	public LoadCounterAbs(	int maximumAllowed, 
							int waterMarkSize, 
							int initialWeight,
							int id,
							int averagePeriod) {
		
		if(averagePeriod < 1000){
			maximumAllowed = maximumAllowed / averagePeriod * 1000;
		}
		init(maximumAllowed,waterMarkSize,initialWeight,id);
	}
	
	/**
	 * 
	 * @param maximumAllowed
	 * @param waterMarkSize percentage of the step size that defines a low load increase threshold that will cause a weight increase
	 * @param initialWeight
	 * @param id This counter Id
	 */
	
	public LoadCounterAbs(	int maximumAllowed, 
							int waterMarkSize, 
							int initialWeight,
							int id) {
		init(maximumAllowed,waterMarkSize,initialWeight,id);
	}
	
	/**
	 * Helper method which is initalized the object.
	 * @param maximumAllowed
	 * @param waterMarkSize
	 * @param initialWeight
	 * @param id
	 */
	private void init(int maximumAllowed, int waterMarkSize, int initialWeight,
			int id) {
		
		 if(maximumAllowed < 10)
	        {
			 // will cahnge the maximum allowed to be 10 as we are working in flying windows
			 // which size by default is 10 - makes the stepSize be minimal 1.
	            if(c_logger.isTraceDebugEnabled())
	                c_logger.traceDebug(this, "init", "Maximum allowed should not be less then 10 - changing automatically to 10");
	            maximumAllowed = 10;
	        }
	     _maximumAllowed = maximumAllowed;
	     _stepSize = _maximumAllowed / 10;
	     
	    _waterMarkSize = _stepSize * waterMarkSize / 100;
		_lastWeight = FREE_WEIGHT - initialWeight;
		_myId = id;

		_myInfo = new StringBuffer();
		_myInfo.append("Parameters:");
		_myInfo.append(PerfUtil.getTypeStr(_myId));
		_myInfo.append(" id = ");
		_myInfo.append(_myId);
		_myInfo.append(" __maximumAllowed = ");
		_myInfo.append(_maximumAllowed);
		_myInfo.append(" _stepSize = ");
		_myInfo.append(_stepSize);
		_myInfo.append(" _lastWeight = ");
		_myInfo.append(_lastWeight);
		_myInfo.append(" _waterMarkSize = ");
		_myInfo.append(_waterMarkSize);
	}
	
	/**
	 * Calculates and returns the current weight according to the current load
	 * and low "watermark"
	 * @param currentLoad
	 * @return true if weight was changed 
	 */
	protected boolean calculateNewWeight(long currentLoad) {
		
		long newWeight;
		long stepValue;
		boolean weightChanged = false;
		
		newWeight = currentLoad / _stepSize;
		_loadAtLastWeightCalc = currentLoad;
		if(newWeight > 10){
			if (newWeight > 11){
				LoadManager.getInstance().setThrowMsgInOverload(true);
			}
			newWeight = 10;
		}
		else if( newWeight < _lastWeight){ 
			// Stair is separate between loads. E.G.
			// _maximumAllowed = 1000
			// _stepSize = 100
			// stairs will be 0, 100, 200, 300.....800, 900, 1000
			// stairValue will be a nearest stair to our currentLoad. 
			// If currentLoad > stairValue -> steps should be increased by one. 
			// 
			stepValue = newWeight * _stepSize;
			
			// Add low watermark to the stairValue in case when
			// load is going down.
			stepValue += _waterMarkSize;
			
			if(currentLoad > stepValue){
				newWeight++;
			}
		}
		
		if(_lastWeight != newWeight){
			_lastWeight = (int)newWeight;
			weightChanged = true;
		}

		return weightChanged;
	}

	/** 
	 * @see com.ibm.ws.sip.container.load.Weighable#getWeight()
	 */
	public int getWeight() {
		return FREE_WEIGHT - _lastWeight;
	}
	
	/** 
	 * @see com.ibm.ws.sip.container.load.Weighable#calculateWeight()
	 */
	public void calculateWeight() { 
		calculateNewWeight(getCurrentLoad());
		reset();
	}
	
	/** @see com.ibm.ws.sip.container.load.Weighable#getCounterID()
	 */
	public int getCounterID() {
		return _myId;
	}
	
	/**
	 * Method which is called after the timer executed to allow derived
	 * classes to perform action if needed
	 */
	abstract void reset();
	
	/**
	 * @see com.ibm.ws.sip.container.load.Weighable#getLoadUsedForLastWeightCalc()
	 */
	public long getLoadUsedForLastWeightCalc(){
		return _loadAtLastWeightCalc;
	}
}
