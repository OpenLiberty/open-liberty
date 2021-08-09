/*******************************************************************************
 * Copyright (c) 2003,2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.channel.resolver.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;

public class SipResolverEventsCounter {

	/**
     * Class Logger. 
     */
    private static final LogMgr c_logger = Log.get(SipResolverEventsCounter.class);
	
	
	private final AtomicIntegerArray _events;
	private final int _windowSize;
	private final int _arrayWindowSize;
	private final int _interval;
	
	private AtomicInteger _lastUpdate;
	
	private AtomicInteger _totalEvents;
	
	private static boolean usePreciseSystemTimer = false;
	

	public static void setUsePreciseSystemTimer(boolean tempUsePreciseSystemTimer) {
        if (c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug("SipResolverEventsCounter: setUsePreciseSystemTimer = " + tempUsePreciseSystemTimer);
        }		
		usePreciseSystemTimer = tempUsePreciseSystemTimer;
	}
	
	/**
	 * Initialize event counter.
	 * 
	 * @param windowSize windows size in seconds
	 * @param interval window is split to intervals
	 */
	public SipResolverEventsCounter(int windowSize, int interval) {
		if (c_logger.isTraceEntryExitEnabled())
			c_logger.traceEntry(this, "SipResolverEventsCounter: entry");
		
		
		_windowSize = windowSize;
		_interval = interval;
		_arrayWindowSize = windowSize + interval;
		_events = new AtomicIntegerArray(_arrayWindowSize / _interval);
		for (int i = 0; i < _events.length(); i++) {
			_events.set(i, 0);
		}
		
		if (usePreciseSystemTimer) {
			// Convert nanoseconds to seconds
			_lastUpdate = new AtomicInteger((int)(System.nanoTime() / 1000000000L));
		}
		else {
			_lastUpdate = new AtomicInteger((int)(System.currentTimeMillis() / 1000L));
		}
		_totalEvents = new AtomicInteger(0);
		
        if (c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug("SipResolverEventsCounter: initialized, window size: " + _windowSize + " interval size: " + _interval + " array size: " + _events.length());
        }		
	}
	
	
	
	/**
	 * Report event to the current time
	 *  
	 * @return number of events to the current window.
	 */
	public int reportEvent() {
		int current = 0;
		
		if (usePreciseSystemTimer) {
			// Convert nanoseconds to seconds
			current = (int)(System.nanoTime() / 1000000000L);
		}
		else {
			current = (int)(System.currentTimeMillis() / 1000L);
		}
		int last = _lastUpdate.get();
		
		// If timestamp changed with more than one interval time then we can erase cells
		// other cases, if we change the last update timestamp then we wouldn't be able
		// to know exactly which cells are timed out.
		if (current - last > _interval) {
    		if (_lastUpdate.compareAndSet(last, current)) {
    			// only one thread will enter here because only one thread was able to
    			// change the timestamp, the other threads will get a false for the set.
    			eraseOldValues(current, last);
    		}
		}
		
		// updating current location according to the array size (window size + interval)
		int loc = (current % _arrayWindowSize) / _interval;
		int val = _events.incrementAndGet(loc);
		int total = _totalEvents.incrementAndGet();
		
        if (c_logger.isTraceDebugEnabled()){
        	c_logger.traceDebug("SipResolverEventsCounter: reportEvent: setting location: " + loc + " to: " + val + " total: " + total);
        }		
		
		return total;
	}
	
	public void reset() {
		_totalEvents.set(0);
		for (int i = 0; i < _events.length(); i++) {
			_events.set(i, 0);
		}		
	}
	
	/**
	 * This method will erase timeout cells.
	 * The array represent the current windows and counts all of the events
	 * every interval of time passes we need to clear the expired cells
	 * 
	 * @param current - current time (in seconds)
	 * @param last - last update time (in seconds)
	 */
    private void eraseOldValues(int current, int last) {
		int num;
		int startLoc;
		if (current - last > _windowSize) {
			// if we get an update after more than the requested window
			// then the whole array is expired and we need to reset it.
			startLoc = (current % _arrayWindowSize) / _interval;
			num = _events.length();
	        if (c_logger.isTraceDebugEnabled()){
	        	c_logger.traceDebug("SipResolverEventsCounter: eraseOldValues: erasing all values.");
	        }		
		} else {
			startLoc = ((last - _windowSize + _interval) % _arrayWindowSize) / _interval;
			num =  current / _interval - last / _interval;
	        if (c_logger.isTraceDebugEnabled()){
	        	c_logger.traceDebug("SipResolverEventsCounter: eraseOldValues: erasing " + num);
	        }		
		}
		
		if (num > 0) {
			int total = 0;
    		for (int i = 0; i < num; i++) {
    			int loc = (i + startLoc) % _events.length();
    			int old = _events.getAndSet(loc, 0);
    			total += old;
    		}
    		int now = _totalEvents.addAndGet(-total);
	        if (c_logger.isTraceDebugEnabled()){
	        	c_logger.traceDebug("SipResolverEventsCounter: eraseOldValues: erasing from " + startLoc + ", " + num + " cells. New total: " + now + " Old total: " + total);
	        }    		
		} else {
			// if no cells were removed we will restore the timestamp
			// since the array is window size + interval then we will always
			// reset at least one cell but i'm leaving this just in case.
			_lastUpdate.set(last);
		}
    }

}
