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
package com.ibm.ws.request.interrupt.internal;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.interrupt.InterruptObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The interrupt manager finds InterruptObject instances in the event tree for
 * the hung request, and drives them, from most recently registered to least
 * recently registered.
 * 
 * If the request has ended, the interrupt manager will stop.
 * 
 * If the hung request manager is stopped, it should call the cancel method
 * on this class to prevent the interrupt manager from continuing to try to
 * interrupt this request.
 */
public class InterruptManager implements Runnable {

	private static final TraceComponent tc = Tr.register (InterruptManager.class, "requestInterrupt" /*, "com.ibm.ws.request.timing.internal.resources.LoggingMessages"*/);
	private static final int REREGISTER_TIME_SECONDS = 5;
	private static final int EXCEPTION_LIMIT = 3;
	
	/**
	 * A string representing this request.
	 */
	private final String requestId;
	
	/**
	 * The stack of interrupt objects.  The stack can change as the request
	 * progresses.
	 */
	private final InterruptibleThreadObject odiStack;

	/**
	 * The scheduled executor that we'll use to schedule ourselves to run
	 * again.
	 */
	private final ScheduledExecutorService scheduledExecutor;

	/**
	 * The ITO can cancel us when the request finishes.
	 */
	private boolean cancelled = false;
	
	/**
	 * Determines if we've finished trying to interrupt (given up).
	 */
	private boolean finished = false;
	
	/**
	 * The number of exceptions that ODIs have thrown to us.
	 */
	private int exceptionCount = 0;
	
	/**
	 * Constructor 
	 * 
	 * @param requestId A string representing the request that we are interrupting.
	 * @param odiStack The stack of InterruptObjects that we can interrupt.
	 * @param ses The scheduled executor we can use to re-schedule ourselves.
	 */
	public InterruptManager(String requestId, InterruptibleThreadObject odiStack, ScheduledExecutorService ses) {
		this.requestId = requestId;
		this.odiStack = odiStack;
		this.scheduledExecutor = ses;
	}

	/**
	 * Try to interrupt the request.  If we can't interrupt the request, 
	 * don't try to reschedule.  If we tried to interrupt, reschedule to 
	 * run again in the future.
	 */
	@Override
	public void run() {
		boolean localFinished = true;
		
		try {
			if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
				Tr.event(tc, "run", requestId);
			}

			// If we were cancelled, exit without doing anything.
			if (cancelled) {
				return;
			}

			// Look for the first available interrupt object
			InterruptObject io = odiStack.findNewestUndrivenInterruptObject();
			if (io != null) {
				// Drive the interrupt object, and try again in 5 seconds.
				if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
					Tr.event(tc, "Driving interrupt object", new Object[] {requestId, io});
				}

				try {
					io.interrupt();
				} catch (Throwable t) {
					FFDCFilter.processException(t, this.getClass().getName(), "95");
					exceptionCount++;
				} finally {
					if (exceptionCount < EXCEPTION_LIMIT) {
						scheduledExecutor.schedule(this, REREGISTER_TIME_SECONDS, TimeUnit.SECONDS);
						localFinished = false;
					} else {
						// TODO: Issue message about not re-scheduling due to exception limit.
						if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
							Tr.event(tc, "Disabling InterruptManager due to exception limit", requestId);
						}
					}
				}
			} else {
				// TODO: Issue done message
				if ((TraceComponent.isAnyTracingEnabled()) && (tc.isEventEnabled())) {
					Tr.event(tc, "Finished interrupting request", requestId);
				}
			}
		} finally {
			finished = localFinished;
		}
	}

	/**
	 * Stop trying to interrupt this request.  The caller is responsible for
	 * removing this interrupt manager from the interrupt managers map.
	 */
	public void cancel() {
		cancelled = true;
	}

	/**
	 * Tells us if we're running or scheduled.
	 */
	public boolean isFinished() {
		return finished;
	}
	
	/**
	 * Gets the request ID that we are monitoring.
	 */
	public String getRequestId() {
		return requestId;
	}
}
