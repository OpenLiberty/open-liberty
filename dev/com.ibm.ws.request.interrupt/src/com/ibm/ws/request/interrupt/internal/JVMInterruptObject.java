/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.request.interrupt.internal;

import com.ibm.websphere.interrupt.InterruptObject;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

/**
 * This class wraps the JVM "interrupt thread" functions in an InterrupObject
 * for use by the ITI/ITO.
 */
final class JVMInterruptObject implements InterruptObject {
	/**
	 * Tracing infrastructure
	 */
	private static final TraceComponent tc = Tr.register(JVMInterruptObject.class,
			"requestInterrupt"/* , "com.ibm.ws.request.interrupt.internal.resources.LoggingMessages" */);

	/**
	 * Have we tried to interrupt yet?
	 */
	private boolean _tried = false;

	/**
	 * The thread for which this ODI is created.
	 */
	private Thread _thread = null;

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

	/**
	 * Constructor
	 */
	protected JVMInterruptObject(Object interruptibleIOContextObject, Method interruptibleIOContextisBlockedMethod,
			Method interruptibleIOContextunblockMethod, Object interruptibleLockContextObject,
			Method interruptibleLockContextisBlockedMethod, Method interruptibleLockContextunblockdMethod) {
		_thread = Thread.currentThread();

		_interruptibleIOContextObject = interruptibleIOContextObject;
		_iOContextisBlockedMethod = interruptibleIOContextisBlockedMethod;
		_iOContextunblockMethod = interruptibleIOContextunblockMethod;

		_interruptibleLockContextObject = interruptibleLockContextObject;
		_lockContextisBlockedMethod = interruptibleLockContextisBlockedMethod;
		_lockContextunblockMethod = interruptibleLockContextunblockdMethod;
	}

	/**
	 * Resets this object for another registration.
	 */
	protected void reset() {
		_tried = false;
	}
	
	  /**
	   * Tells if the thread is locked up in a JVM net or wait block.
	   */
	  protected boolean isBlocked()
	  {
		boolean threadIsBlocked = false;

		/* call the isBlocked method on the lock context.
		 */
		if ((_interruptibleLockContextObject != null) && (_lockContextisBlockedMethod != null)
				&& (threadIsBlocked == false)) {
			Boolean booleanObject;
			try {
				booleanObject = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Boolean>() {
							public Boolean run() throws Exception {
								return (Boolean) _lockContextisBlockedMethod.invoke(_interruptibleLockContextObject, new Object[] {});
								
							}
						}
						);

				if (booleanObject.booleanValue()) {
					threadIsBlocked = true;
				}
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception invoking isBlocked on lock context", e);
				}
			}
		}

		/* call the isBlocked method on the IO context.
		 */
		if ((_interruptibleIOContextObject != null) && (_iOContextisBlockedMethod != null)
				&& (threadIsBlocked == false)) {
			Boolean booleanObject;
			try {
				booleanObject = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Boolean>() {
							public Boolean run() throws Exception {
								return (Boolean) _iOContextisBlockedMethod.invoke(_interruptibleIOContextObject, new Object[] {});
								
							}
						}
						);
				if (booleanObject.booleanValue()) {
					threadIsBlocked = true;
				}
			} catch (Exception e1) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception invoking isBlocked on IO context", e1);
				}
			}
		}	    
	    return threadIsBlocked;
	  }	

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.websphere.interrupt.InterruptObject#interrupt()
	 */
	@Override
	public boolean interrupt() {
		/*-------------------------------------------------------------------*/
		/* Set the bit that indicates this interrupt has been driven. */
		/*-------------------------------------------------------------------*/
		_tried = true;
		boolean threadIsBlocked = false;

		if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
			Tr.debug(tc, "interrupt method stack trace of thread. ", new Object[] { _thread, _thread.getStackTrace() });
		}

		/* call the isBlocked method on the lock context.
		 * If thread is blocked call the unblock method. 
		 */
		if ((_interruptibleLockContextObject != null) && (_lockContextisBlockedMethod != null)
				&& (_lockContextunblockMethod != null)) {
			Boolean booleanObject;
			try {
				booleanObject = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Boolean>() {
							public Boolean run() throws Exception {
								return (Boolean) _lockContextisBlockedMethod.invoke(_interruptibleLockContextObject, new Object[] {});
								
							}
						}
						);

				if (booleanObject.booleanValue()) {
					threadIsBlocked = true;
					if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
						Tr.debug(tc, "calling unblock on lock context", _thread);
					}
					try {
						AccessController.doPrivileged(
								new PrivilegedExceptionAction<Object>() {
									public Object run() throws Exception {
										_lockContextunblockMethod.invoke(_interruptibleLockContextObject, new Object[] {});
										return null;
									}
								}
								);
					} catch (Exception e) {
						if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
							Tr.debug(tc, "Exception invoking unblock on lock context", e);
						}
					}
				}
			} catch (Exception e) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception invoking isBlocked on lock context", e);
				}

			}
		}

		/* call the isBlocked method on the IO context.
		 * If thread is blocked call the unblock method. 
		 */
		if ((_interruptibleIOContextObject != null) && (_iOContextisBlockedMethod != null)
				&& (_iOContextunblockMethod != null)) {
			Boolean booleanObject;
			try {
				booleanObject = AccessController.doPrivileged(
						new PrivilegedExceptionAction<Boolean>() {
							public Boolean run() throws Exception {
								return (Boolean) _iOContextisBlockedMethod.invoke(_interruptibleIOContextObject, new Object[] {});
								
							}
						}
						);
				if (booleanObject.booleanValue()) {
					threadIsBlocked = true;
					if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
						Tr.debug(tc, "calling unblock on IO context", _thread);
					}
					try {
						AccessController.doPrivileged(
								new PrivilegedExceptionAction<Object>() {
									public Object run() throws Exception {
										_iOContextunblockMethod.invoke(_interruptibleIOContextObject, new Object[] {});
										return null;
									}
								}
								);
					} catch (Exception e2) {
						if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
							Tr.debug(tc, "Exception invoking unblock on IO context", e2);
						}
					}
				}
			} catch (Exception e1) {
				if (TraceComponent.isAnyTracingEnabled() && (tc.isDebugEnabled())) {
					Tr.debug(tc, "Exception invoking isBlocked on IO context", e1);
				}
			}
		}
		return threadIsBlocked;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.websphere.interrupt.InterruptObject#queryTried()
	 */
	@Override
	public boolean queryTried() {
		return _tried;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.websphere.interrupt.InterruptObject#getName()
	 */
	@Override
	public String getName() {
		return "JVM Interrupt Object";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ibm.websphere.interrupt.InterruptObject#getDisplayInfo()
	 */
	@Override
	public String getDisplayInfo() {
		return (isBlocked() ? "Thread is blocked" : "Thread is not blocked");
	}

}