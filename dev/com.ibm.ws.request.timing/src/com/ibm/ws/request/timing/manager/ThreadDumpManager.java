/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.request.timing.manager;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.request.timing.RequestTimingService;

/**
 * This class will be used to schedule task to create thread dump.
 * Input required 
 * (1) No of Thread dumps required
 * (2) Time difference between 2 thread dump generated (Minutes) *
 */
public class ThreadDumpManager {
	
	private static final TraceComponent tc = Tr.register (ThreadDumpManager.class, "requestTiming", "com.ibm.ws.request.timing.internal.resources.LoggingMessages");

	private final int threadDumpsRequired;

	private final int threadDumpDuration;

	/**
	 * This counter also serves as the lock for scheduling the thread dump manager.
	 */
	private final Counter threadDumpsGenerated = new Counter();

	/**
	 * Must be synchronized around threadDumpsGenerated when using this object.
	 */
	private volatile ScheduledFuture<?> timeKeeper = null;
	
	public ThreadDumpManager(int threadDumpsRequired, int threadDumpDuration){
		this.threadDumpsRequired = threadDumpsRequired;
		this.threadDumpDuration = threadDumpDuration;
	}

	public void startTimer() {
		//Start the task to create javacore if not already started
		//Only a single instance of this task should be running at any point of time
		synchronized(threadDumpsGenerated) {
			if(timeKeeper == null){
				threadDumpsGenerated.reset();
				timeKeeper = RequestTimingService.getScheduledExecutorService().scheduleAtFixedRate(generateThreadDump, 0, threadDumpDuration, TimeUnit.MINUTES);
				if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
					Tr.debug(tc, "Starting thread dump scheduler with initial delay (ms) : " + 0 + " and period (min) : " + threadDumpDuration);
				}
			}
			
			// If we have not completed the first dump yet, make a reasonable attempt
			// to wait for it to complete.
			if (threadDumpsGenerated.get() == 0) {
				try {
					threadDumpsGenerated.wait();
				} catch (InterruptedException ie) {
					FFDCFilter.processException(ie, this.getClass().getName(), "77", this);
				}
			}
		}
	}

	public void stopTimer() {
		synchronized(threadDumpsGenerated){
			if(timeKeeper != null){
				try {
					timeKeeper.cancel(false);
					timeKeeper = null;
				
					if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
						Tr.debug(tc, "Stopping thread dump scheduler.");
					}
				} finally {
					// Wake up anyone who may have been waiting for the first dump to occur, if
					// we did not take the first dump yet.
					if (threadDumpsGenerated.get() == 0) {
						threadDumpsGenerated.notifyAll();
					}
				}
			}
		}
	}

	/**
	 * Scheduled executor will invoke this runnable at a fixed interval:     * 
	 */
	private final Runnable generateThreadDump = new Runnable() {
		@Trivial
		@Override
		public void run() {
			try {
				RequestTimingService.getLibertyProcess().createJavaDump(new HashSet<String>(Arrays.asList("thread")));
			} finally {
				synchronized(threadDumpsGenerated) {
					// If we finished the first dump, notify everyone who was waiting.
					if (threadDumpsGenerated.get() == 0) {
						threadDumpsGenerated.notifyAll();
					}
					// If we've taken as many dumps as we said we would, stop.
					threadDumpsGenerated.increment();
					if(threadDumpsGenerated.get() >= threadDumpsRequired){
						stopTimer();
					}
				}
			}
		}
	};
	
	private static final class Counter {
		int count = 0;
		public void increment() { count++; }
		public int get() { return count; }
		public void reset() { count = 0; }
	}
}

