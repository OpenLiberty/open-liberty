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

/**
 * @author anat
 * Represents Array of values that is filled in loop and knows to return average 
 * according to the flying window size defined when constructed.
 */
public class CounterArray {

	/**
	 * Class Logger. 
	 */
	private static final LogMgr c_logger = Log.get(CounterArray.class);
	
	/**
	 * Cells array. Each cell represents 1 sec result.
	 * Initiated when constructed.
	 */
	private long _cellsArray[];
	
	/**
	 * Size of the _callsArray. Minimum size can be 1o sec.
	 */
	private int _cellsArraySize = MINIMUM_CELLS_ARRAY_SIZE;
	
	/**
	 * Minimum is 10 sec when each cell will be 1 sec.
	 */
	private static final int MINIMUM_CELLS_ARRAY_SIZE = 10;

	/**
	 * Index to the last cell where result was updated. 
	 */
	private int _lastUpdatedIndex = -1;
	
	
	/**
	 * CounterArray will calculate average of messages in 1 sec (summarize 
	 * content of all cells and divide it by _cellsArraySize ) and then 
	 * multiply it by _averagePeriod
	 */
	private int _averagePeriod;

	/**
	 * Result of the last calculated summary of all cells.
	 */
	private long _lastSummery = -1;
	
	/**
	 * Result of the last calculated average.
	 */
	private long _lastAverage = -1;
	
	/**
	 * _isArrayFull will be true after the first time when whole array filled
	 * and loop started. Used to calculate average before then array fully 
	 * filled at the beginning.
	 * 
	 */
	private boolean _isArrayFull = false;
	
	private StringBuffer _myInfo = new StringBuffer();

	/**
	 * 
	 * @param averagePeriodMsec
	 */
	public CounterArray(int averagePeriodMsec) {
		// in msec - need to be converted to sec as we 
		// defined 1 cell as 1 sec. 
		if(averagePeriodMsec < 1000){
			_averagePeriod = 1;
		}
		else {
			_averagePeriod = averagePeriodMsec / 1000;
		}
		
		//If _averagePeriod is higher than MINIMUM_CELLS_ARRAY_SIZE the 
		// _cellsArray will have size like _averagePeriod. In this case
		// getAverage() will return sum of all cells.
		if(_averagePeriod > MINIMUM_CELLS_ARRAY_SIZE){
			_cellsArraySize = _averagePeriod;
		}
		_cellsArray = new long [_cellsArraySize];
		
		_myInfo.append(" AveragePeriod = ");
		_myInfo.append(_averagePeriod);
		_myInfo.append(" CellsArraySize = ");
		_myInfo.append(_cellsArraySize);
	}

	/**
	 * Returns the average calculated according to the _averagePeriod.
	 */
	public long getAverage() {
		calculateAverage();
		return _lastAverage;
	}

	/**
	 * Adds counter value (for last sec) to the next cell in array.
	 * @param conter
	 */
	public void addCounter(long counter) {
		
		if(_lastUpdatedIndex < _cellsArraySize -1 ){
			// When we didn't reach the end
			++_lastUpdatedIndex;
		}
		else{
			if(_isArrayFull == false){
				_isArrayFull = true;
			}
			// If the end of the _cellsArray reached - start from the beginning
			_lastUpdatedIndex = 0;
		}
		calculateSummery(_lastUpdatedIndex,counter);
		_cellsArray[_lastUpdatedIndex] = counter;		
	}
	
	/**
	 *  @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < _cellsArray.length; i++) {
			buff.append("[");
			buff.append(_cellsArray[i]);
			buff.append("]");			
		}
		return buff.toString();
	}
	
	/**
	 * Helper method which calculates the current new summery.
	 */
	private void calculateSummery(int indextToReplace,long counter){
		
		if(_lastSummery == -1){
			// This is the first time we are calculating the average.
			_lastSummery = counter;		}
		else{
			if(!_isArrayFull){
				// Because the next cell is empty - counter should be added
				// to the _lastSummery as is.
				_lastSummery += counter;				
			}
			else { //_isArrayFull = true
				
				// Need to remove the value of the cell that is going to be
				// replaced from the _lastSummery and the add the value of the
				// new counter.
				_lastSummery -= _cellsArray[indextToReplace];
				_lastSummery += counter;
			}
		}
	}
	
	
	/**
	 * Helper method which calculates the average.
	 */
	private void calculateAverage() {
		
		if (!_isArrayFull) {
			// Only if _averagePeriod is less than the_cellsArraySize
			// and number filled cells is higher than _averagePeriodof -
			// real average should be calculated. Otherwise _lastSummery
			// calculate before is enough.
			if (_averagePeriod < _cellsArraySize
					&& _lastUpdatedIndex >= _averagePeriod) {
				// Calculate average of filled cells only * _averagePeriod
				_lastAverage = (_lastSummery / (_lastUpdatedIndex + 1));
				_lastAverage *= _averagePeriod;
			} 
			else {
				_lastAverage = _lastSummery;
			}
		} 
		else { // _isArrayFull = true

			if (_cellsArraySize > MINIMUM_CELLS_ARRAY_SIZE) {
				// In this case _lastAverage is exactly the _lastSummery.
				// Because the _cellsArraySize size is exactly the
				// _averagePeriod value.
				_lastAverage = _lastSummery;
			} 
			else {
				// calculate average per _averagePeriod;
				_lastAverage = (_lastSummery / _cellsArraySize) * _averagePeriod;
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	public String getCurrentState() {
		StringBuffer buff = new StringBuffer();
		buff.append(_myInfo);
		buff.append(" LastAverage = ");
		buff.append(_lastAverage);
		buff.append(" LastSummery = ");
		buff.append(_lastSummery);
		buff.append(" LastUpdatedIndex = ");
		buff.append(_lastUpdatedIndex);
		buff.append(" \n\r Array: \n\r ");
		
		for (int i = 0; i < _cellsArray.length; i++) {
			buff.append(_cellsArray[i]);
			buff.append(" , ");
		}
		return buff.toString();
		
	}
}
