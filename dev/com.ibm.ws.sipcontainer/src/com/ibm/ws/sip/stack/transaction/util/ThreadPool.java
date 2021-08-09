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
package com.ibm.ws.sip.stack.transaction.util;

import java.util.LinkedList;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;

/**
 * event dispatcher
 */
public class ThreadPool implements Runnable
{
	/** the singleton instance */
	private static ThreadPool s_instance = new ThreadPool();

	/** class Logger */
	private static final LogMgr s_logger = Log.get(ThreadPool.class);

	/** queue of events */
	private LinkedList m_queue;
    
	/**
	 * number of threads. default is 0, meaning no dispatching (all
	 * events are executed directly from the network/timer threads).
	 */
	private static final int s_numberOfThreads = SIPTransactionStack.instance().getConfiguration().getNumberOfApplicationThreads();
    
	/** pool of application threads, or null if number of threads is 0 */
	private final Thread[] m_threads;

	/**
	 * instantiate pool
	 */
	public static ThreadPool instance() {
		return s_instance;
	}

	/**
	 * private constructor
	 */
	private ThreadPool() {
		m_queue = new LinkedList();

		m_threads = s_numberOfThreads == 0 ? null : new Thread[s_numberOfThreads];
		if (m_threads != null) {
			for (int i = 0; i < m_threads.length; i++) {
				Thread thread = new Thread(this, "SipAppThread-" + i);
				thread.setDaemon(true);
				thread.start();
			}
		}
	}

	/**
	 * queues a new event.
	 * may be called from any thread.
	 * 
	 * @param event the new event to be added to the queue
	 */
	public void invoke(Runnable event) {
		if (event == null) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Error: null event queued in ThreadPool");
			}
			return;
		}
		if (m_threads == null) {
			// no thread pool. execute directly from the calling (stack) thread
			invokeProtected(event);
		}
		else synchronized (this) {
			m_queue.addLast(event);
			notify();
		}
	}

	/**
	 * queues a new event for high priority execution.
	 * may be called from any thread.
	 * 
	 * @param event the new event to be added to the queue
	 */
	public void invokeImmediately(Runnable event) {
		if (event == null) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Error: null event queued (immediately) in ThreadPool");
			}
			return;
		}
		if (m_threads == null) {
			// no thread pool. execute directly from the calling (stack) thread
			invokeProtected(event);
		}
		else synchronized (this) {
			m_queue.addLast(event);
			notify();
		}
	}

	/**
	 * waits for new events to be queued, and executes those
	 * events serially
	 */
	public void run() {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("ThreadPool thread started");
		}
		try {
			while (true) {
				// fetch next queued event
				Runnable event;
				synchronized (this) {
					if (m_queue.isEmpty()) {
						wait();
					}
					event = m_queue.isEmpty() ?
						null
						: (Runnable)m_queue.removeFirst();
				}
				if (event == null) {
					if (s_logger.isTraceDebugEnabled()) {
						s_logger.traceDebug("Error: ThreadPool awakened for no reason");
					}
				}
				else {
					// execute the event
					if (s_logger.isTraceDebugEnabled()) {
						s_logger.traceDebug("in event.run");
					}
					invokeProtected(event);
					if (s_logger.isTraceDebugEnabled()) {
						s_logger.traceDebug("out event.run");
					}
					event = null;
				}
			}
		}
		catch (InterruptedException e) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "run", "InterruptedException", e);
			}
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("ThreadPool thread terminated");
		}
	}

	/**
	 * invoke given event. catch and log exceptions.
	 * @param event event to invoke
	 */
	private void invokeProtected(Runnable event) {
		try {
			event.run();
		}
		catch (Exception e) {
			FFDCFilter.processException(e,
				"com.ibm.ws.sip.stack.transaction.util.ThreadPool.invokeProtected",
				"1", this); 
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug("Unhandled exception in SIP stack invocation thread");
				s_logger.traceDebug(this, "invokeProtected", "Exception", e);
			}
		}
	}
}
