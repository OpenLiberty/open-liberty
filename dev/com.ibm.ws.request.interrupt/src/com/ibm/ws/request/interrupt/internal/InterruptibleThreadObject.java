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

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import com.ibm.websphere.interrupt.InterruptObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.request.interrupt.status.InterruptibleThreadObjectOdiStatus;
import com.ibm.ws.request.interrupt.status.InterruptibleThreadObjectStatus;

/**
 * This class holds the state of the interruptible request for
 * the current request/thread.
 * 
 * Most methods are synchronized.  In practice this is never an issue as the only
 * time concurrent access should occur is when a request is hung and we are trying
 * to drive ODIs, which is not a performance path.
 */
public class InterruptibleThreadObject {

	private static final TraceComponent tc = Tr.register (InterruptibleThreadObject.class, "requestInterrupt" /*, "com.ibm.ws.request.timing.internal.resources.LoggingMessages"*/);

	/**
	 * Stack of currently registered interrupt objects.
	 */
	private Deque<InterruptObject> odis = new LinkedList<InterruptObject>();
	
	/**
	 * Class which monitors JVM network activity.
	 */
	private Class<?> _interruptibleIOContextClass = null;

	/**
	 * Object which monitors JVM network activity.
	 */
	private Object _interruptibleIOContextObject = null;   
	
	/**
	 * IO context isBlocked method
	 */
	private Method _iOContextisBlockedMethod = null;

	/**
	 * IO context unblock method
	 */
	private Method _iOContextunblockMethod = null;

	/**
	 * Class which monitors JVM locking activity.
	 */
	private Class<?> _interruptibleLockContextClass = null;

	/**
	 * Object which monitors JVM locking activity.
	 */

	private Object _interruptibleLockContextObject = null;   
	
	/**
	 * Lock context isBlocked method
	 */
	private Method _lockContextisBlockedMethod = null;

	/**
	 * Lock context unblock method
	 */
	private Method _lockContextunblockMethod = null;
	
	private JVMInterruptObject jvmInterruptObject = null;	
	
	/**
	 * Is this thread currently set up to register ODIs?
	 */
	private boolean isReady = false;
	
	/**
	 * The request ID for the current request, or null if none.
	 */
	private String requestId = null;
	
	/**
	 * The interrupt manager for the request running on the thread represented
	 * by this interrupt object.  This can be null when the request is not hung.
	 */
	private InterruptManager currentInterruptManager = null;
	
	/**
	 * The thread ID which we are monitoring.
	 */
	private final long threadId; 

	/**
	 * The time when we started tracking the current request.
	 */
	private Calendar dispatchStartTime = null;
	
	/**
	 * Constructor
	 * 
	 * @param interruptibleIOContext interruptible IO context
	 * @param interruptibleLockContext interruptible lock context
	 */
	public InterruptibleThreadObject(Class<?> interruptibleIOContextClass, Class<?> interruptibleLockContextClass) {
			
		threadId = Thread.currentThread().getId();
		if (interruptibleIOContextClass != null) {
			_interruptibleIOContextClass = interruptibleIOContextClass;
			try {
				_interruptibleIOContextObject = _interruptibleIOContextClass.newInstance();
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception getting object for interruptible IO Context Class", e);
				}
			} 
			try {
				_iOContextisBlockedMethod = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Method>() {
							public Method run() throws Exception {
								return _interruptibleIOContextClass.getMethod("isBlocked", new Class<?>[] {});
							}
						});
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception getting method isBlocked on IO context", e);
				}
			} 
			try {
				_iOContextunblockMethod = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Method>() {
							public Method run() throws Exception {
								return _interruptibleIOContextClass.getMethod("unblock", new Class<?>[] {});
							}
						});
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception getting method unblock on IO context", e);
				}
			} 
		}
		
		if (interruptibleLockContextClass != null) {
			_interruptibleLockContextClass = interruptibleLockContextClass;
			try {
				_interruptibleLockContextObject = _interruptibleLockContextClass.newInstance();
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception getting object for interruptible lock Context Class", e);
				}
			} 
			try {
				_lockContextisBlockedMethod = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Method>() {
							public Method run() throws Exception {
								return _interruptibleLockContextClass.getMethod("isBlocked", new Class<?>[] {});
							}
						});
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception getting method isBlocked on lock context", e);
				}
			} 
			try {
				_lockContextunblockMethod = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Method>() {
							public Method run() throws Exception {
								return _interruptibleLockContextClass.getMethod("unblock", new Class<?>[] {});
							}
						});
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception getting method unblock on lock context", e);
				}
			}	
		}

		if (((_interruptibleIOContextObject != null) && ((_iOContextisBlockedMethod != null) || (_iOContextunblockMethod != null))) ||
				((_interruptibleLockContextObject != null) && ((_lockContextisBlockedMethod != null) || (_lockContextunblockMethod != null)))) {
			jvmInterruptObject = new JVMInterruptObject(_interruptibleIOContextObject, _iOContextisBlockedMethod, _iOContextunblockMethod,
					_interruptibleLockContextObject, _lockContextisBlockedMethod, _lockContextunblockMethod); 	
		}		
	}
	
	/**
	 * Clear the stack at the beginning or end of a request.
	 * 
	 * @param newRequest true if a new request is beginning, false if not.
	 * @param requestId A string representing the request that is starting or ending.
	 */
	public synchronized void clear(boolean newRequest, String requestId) {

		if (Thread.currentThread().getId() != threadId) {
			throw new IllegalStateException("An attempt was made to clear this InterruptibleThreadObject from a thread ID other than " + threadId);
		}
			
		/* deregister the jvm interrupt object */
		if (newRequest == false) {
			if (jvmInterruptObject != null) {
				deregister(jvmInterruptObject);
			}
		}
		// Complain if the ODI stack is not empty.
		if (odis.isEmpty() == false) {
			StringBuilder sb = new StringBuilder();
			for (InterruptObject io : odis) {
				sb.append(io.getName());
				sb.append(" :: ");
			}
			String requestType = (newRequest ? "start" : "end");
			Exception e = new IllegalStateException("The following interrupt objects were present at request " + requestType + ": " + sb.toString());
			FFDCFilter.processException(e, this.getClass().getName(), "42", this);
		}

		odis.clear();
		isReady = newRequest;

		if (newRequest) {
			if (this.requestId != null) {
				IllegalStateException ise = new IllegalStateException("A new request was detected but this InterruptObject was already registered with request ID " + this.requestId);
				FFDCFilter.processException(ise, this.getClass().getName(), "73", this);
			}
			
			this.requestId = requestId;
			this.dispatchStartTime = Calendar.getInstance();
		} else {
			if ((this.requestId == null) || (this.requestId.equals(requestId) == false)) {
				IllegalStateException ise = new IllegalStateException("Request " + requestId + " was ending but this InterruptObject was registered with request ID " + this.requestId);
				FFDCFilter.processException(ise, this.getClass().getName(), "80", this);
			}

			this.requestId = null;
			this.dispatchStartTime = null;
		}
		
		// Cancel any interrupt manager for the previous request on this thread.
		if (currentInterruptManager != null) {
			currentInterruptManager.cancel();
			currentInterruptManager = null;
		}
		if (newRequest == true) {
			if (jvmInterruptObject != null) {
				jvmInterruptObject.reset();
				register(jvmInterruptObject);
			}
		}
	}
	
	/**
	 * Tell if this interrupt object is currently accepting ODIs for the current request.
	 */
	public boolean isReady() {
		return isReady;
	}
	
	/**
	 * Register an ODI for the current request.
	 */
	public synchronized void register(InterruptObject odi) {
		if (isReady == false) {
			throw new IllegalStateException("The current thread is not currently accepting InterruptObject registration");
		}
		
		if (odi == null) {
			throw new IllegalArgumentException("A null InterruptObject was supplied");
		}
		
		odis.addFirst(odi);
	}
	
	/**
	 * Deregister an ODI from the current request.
	 */
	public synchronized void deregister(InterruptObject odi) {
		if (isReady == false) {
			throw new IllegalStateException("The current thread is not currently accepting InterruptObject deregistration");
		}

		if (odi == null) {
			throw new IllegalArgumentException("A null InterruptObject was supplied");
		}

		// To retain compatibility with tWAS, remove the ODI and any ODIs above it
		// in the stack.
		if (odis.contains(odi)) {
			for (InterruptObject cur = null; cur != odi;) {
				cur = odis.removeFirst();
			}
		} else {
			// Just complain... tWAS did not throw an exception here.
			IllegalArgumentException iae = new IllegalArgumentException("This InterruptObject was not currently registered: " + odi.toString());
			FFDCFilter.processException(iae, this.getClass().getName(), "84", this);
		}
	}
	
	/**
	 * Find the first available InterruptObject in the ODI stack.
	 */
	synchronized InterruptObject findNewestUndrivenInterruptObject() {
		InterruptObject firstUndrivenOdi = null;
		
		// Find the newest undriven interrupt object, and drive it.
		for (InterruptObject odi : odis) {
			if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
				Tr.debug(tc, "Checking to see if ODI has been driven", odi);
			}

			if (odi.queryTried() == false) {
				firstUndrivenOdi = odi;
				break;
			}
		}
		
		return firstUndrivenOdi;
	}
	
	/**
	 * Interrupts the request currently running on the thread managed by this
	 * interrupt object.
	 */
	synchronized void interruptCurrentRequest(String requestId, ScheduledExecutorService scheduledExecutor) {
		if (currentInterruptManager == null) {
			InterruptManager im = new InterruptManager(requestId, this, scheduledExecutor);
			currentInterruptManager = im;
			scheduledExecutor.execute(im);
		} else {
			// FFDC, we can't have a second interrupt manager.
			Exception ise = new IllegalStateException("Cannot create an InterruptManager for request " + requestId + ", because this InterruptObject already has an InterruptManager scheduled for request " + currentInterruptManager.getRequestId());
			FFDCFilter.processException(ise, this.getClass().getName(), "142", this);
		}
	}
	
	/**
	 * Build a object that represents the current state of this ITO.
	 * If this ITO is not currently monitoring a request, no status is returned.
	 * 
	 * @return A object if this ITO is monitoring a request, or null if not.
	 */
	synchronized InterruptibleThreadObjectStatus getStatus() {
		// Leave now if there's nothing to report.
		if (isReady == false) {
			return null;
		}
		InterruptibleThreadObjectStatus status = new InterruptibleThreadObjectStatus(threadId,
				                                                                     requestId,
				                                                                     (currentInterruptManager != null) ? Boolean.TRUE : Boolean.FALSE,
						                                                             (currentInterruptManager != null) ? currentInterruptManager.isFinished() : false,
								                                                     DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.FULL).format(dispatchStartTime.getTime()),
								                                                     dispatchStartTime.getTimeInMillis());
		List<InterruptibleThreadObjectOdiStatus> odiStatusList = new ArrayList<InterruptibleThreadObjectOdiStatus>();
		int currentPosition = 0;
		for (InterruptObject odi : odis) {
			InterruptibleThreadObjectOdiStatus odiStatus = new InterruptibleThreadObjectOdiStatus(currentPosition++,
					                                                                              odi.getName(), 
					                                                                              odi.getDisplayInfo(), 
					                                                                              odi.queryTried());

			odiStatusList.add(odiStatus);
		}
		status.addOdiStatus(odiStatusList);
		
		return status;
	}
}