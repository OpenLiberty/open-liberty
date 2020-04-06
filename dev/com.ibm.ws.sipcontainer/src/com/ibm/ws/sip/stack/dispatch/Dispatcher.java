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
package com.ibm.ws.sip.stack.dispatch;

import java.util.LinkedList;

import com.ibm.sip.util.log.Log;
import com.ibm.sip.util.log.LogMgr;
import com.ibm.sip.util.log.Situation;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sip.properties.StackProperties;
import com.ibm.ws.sip.stack.transaction.SIPTransactionStack;
import com.ibm.ws.sip.stack.transaction.transport.TransportCommLayerMgr;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SIPListenningConnection;
import com.ibm.ws.sip.stack.transaction.transport.connections.SipMessageByteBuffer;
import com.ibm.ws.sip.stack.transaction.util.ApplicationProperties;
import com.ibm.ws.sip.stack.transaction.util.Debug;
import com.ibm.ws.sip.stack.util.ThreadLocalStorage;

/**
 * singleton class that multiplexes events coming in from the network/timer threads
 * and dispatches them to a separate thread.
 * 
 * @author ran
 */
public class Dispatcher implements Runnable
{
	/** the class logger */
	private static final LogMgr s_logger = Log.get(Dispatcher.class);

	/** singleton instance */
	private static Dispatcher s_instance = new Dispatcher();

	/** queue of pending events */
	private LinkedList m_events;
	
	/**
	 * maximum expected number of objects in queue. dispatch will continue to 
	 * put and remove events from queue but will regard itself as overloaded. 
	 * other threads adding events to queue need to behave according to this
	 * limit and decide whether or not to add new events to queue. 
	 * a value lower then 1 indicates unlimited size. in the config use a value
	 * 0, as a value of -1 is returned for non existing config settings.
	 */
	private int m_maxQueueSize;
	
	/**
	 * flag indicating that we are currently in overloaded mode
	 * @see m_maxQueueSize
	 */
	private boolean m_isOverLoaded;
	
	/**
	 * last time an overload message has been printed to log.
	 * when overloaded we want to print a message to the log
	 * but we need to avoid filling the log with such message.
	 * so we print periodically every 60 seconds if needed
	 */
	private long m_lastOverLoadMessageTime;
	
	/**
	 * for every overload warning, there should be no more than one message
	 * informing about normal state
	 */
	private boolean m_overloadLogged;
    
	/**
	 * number of dispatch threads. default is 0, meaning no dispatching (all
	 * events are executed directly from the network/timer threads).
	 */
	private static final int s_numberOfThreads = SIPTransactionStack.instance().
			getConfiguration().getNumberOfDispatchThreads();
    
	/** pool of dispatch threads, or null if number of threads is 0 */
	private final Thread[] m_threads;

	/** statistics reporting - interval, in milliseconds, for reporting statistics */
	private static int s_reportInterval = ApplicationProperties.getProperties().
		getInt(StackProperties.TIMER_STAT_REPORT_INTERVAL);

	/** statistics reporting - last report time */
	private static long s_lastReport = 0;
	
	/** statistics reporting - total queued events */
	private static int s_nHistory = 0;
	
	/** statistics reporting - peak queued events in the current reporting interval */
	private static int s_peak = 0;
	
	/** statistics reporting - number of event types */
	private static final int N_EVENT_TYPES = 18;
	
	/** statistics reporting - current queued events per event type */
	private static int[] s_nQueuedDist = new int[N_EVENT_TYPES];
	
	/** statistics reporting - total queued events per event type */
	private static int[] s_nHistoryDist = new int[N_EVENT_TYPES];

	/** statistics reporting - lock for synchronizing the counters */
	private static final Object s_reportLock = new Object();

	/**
	 * @return singleton instance
	 */
	public static Dispatcher instance() {
		return s_instance;
	}
	
	/**
	 * private constructor.
	 * starts the dispatch threads.
	 */
	private Dispatcher() {
		m_events = new LinkedList();
		m_threads = s_numberOfThreads == 0 ? null : new Thread[s_numberOfThreads];

		m_maxQueueSize = ApplicationProperties.getProperties().getInt(
			StackProperties.MAX_DISPATCH_Q_SIZE);

		m_isOverLoaded = false;
		
		m_lastOverLoadMessageTime = 0;
		m_overloadLogged = false;
		
		if (m_threads != null) {
			for (int i = 0; i < m_threads.length; i++) {
				Thread thread = new Thread(this, "SIP Stack Dispatch-" + i);
				thread.setDaemon(true);
				thread.start();
			}
		}
	}
	
	/**
	 * waits for new events to be queued, and executes those
	 * events serially
	 */
	public void run() {
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("SIP stack dispatch thread started");
		}
		try {
			while (true) {
				// fetch next queued event
				Event event;
				synchronized(this) {
					if (m_events.isEmpty()) {
						wait();
					}
					event = m_events.isEmpty()
						? null
						: (Event)m_events.removeFirst();
				}
				if (event == null) {
					if (s_logger.isTraceDebugEnabled()) {
						s_logger.traceDebug("Error: SIP dispatch awakened for no reason");
					}
				}
				else {
					if (s_reportInterval > 0) {
						// statistics
						int eventType = eventType(event);
						synchronized(s_reportLock) {
							s_nQueuedDist[eventType]--;
						}
						report(event);
					}
					// execute the event
					try {
						event.onExecute();
					}
					catch (Exception e) {
						FFDCFilter.processException(e, "com.ibm.ws.sip.stack.dispatch.Dispatcher.run", "1", this); 
						if (s_logger.isTraceDebugEnabled()) {
							s_logger.traceDebug("Unhandled exception in SIP stack dispatch thread");
							s_logger.traceDebug("SIP stack dispatch will try to continue normally");
							s_logger.traceDebug(this, "run", "Exception", e);
						}
					}
				}
			}
		}
		catch (InterruptedException e) {
			if (s_logger.isTraceDebugEnabled()) {
				s_logger.traceDebug(this, "run", "InterruptedException", e);
			}
		}
		if (s_logger.isTraceDebugEnabled()) {
			s_logger.traceDebug("SIP stack dispatch thread terminated");
		}
	}

	/**
	 * queues a new event.
	 * may be called from any thread.
	 * 
	 * @param event the new event to be added to the queue
	 */
	private void queue(Event event) {
		if (s_reportInterval > 0) {
			int eventType = eventType(event);
			synchronized(s_reportLock) {
				s_nHistory++;
				s_nHistoryDist[eventType]++;
				s_nQueuedDist[eventType]++;
			}
			int currentSize = m_events.size();
			if (currentSize > s_peak) {
				s_peak = currentSize;
			}
			report(event);
		}
		synchronized(this) {
			m_events.addLast(event);
			notify();
		}

		// check and switch overloaded flag if needed
		if (m_maxQueueSize > 0) {
			boolean isOverLoadedNow = m_events.size() > m_maxQueueSize;
			if (s_logger.isWarnEnabled() && !m_isOverLoaded && isOverLoadedNow) {
				// entering overload mode
				// print message to log every 60 seconds only
				long now = System.currentTimeMillis();
				if (m_lastOverLoadMessageTime + 60000 < now) {
					s_logger.warn("warn.dispatch.queue.overloaded", 
						Situation.SITUATION_REPORT_PERFORMANCE, 
						new Object[] { Integer.valueOf(m_maxQueueSize) });
					m_lastOverLoadMessageTime = now;
					m_overloadLogged = true;
				}
			}
			else if (s_logger.isInfoEnabled() && m_isOverLoaded && !isOverLoadedNow) {
				// exiting overload mode
				if (m_overloadLogged) {
					m_overloadLogged = false;
					s_logger.info("info.dispatch.queue.normal", 
						Situation.SITUATION_REPORT_PERFORMANCE, 
						null); 
				}
			}
			m_isOverLoaded = isOverLoadedNow;
		}
	}

	/**
	 * queues a new event when data arrives from the network
	 * 
	 * @param buffer buffer with a copy of the received bytes.
	 *  this buffer should be returned to the pool when no longer needed.
	 * @param source transport source
	 */
	public void queueIncomingDataEvent(SipMessageByteBuffer buffer, SIPConnection source) {
		if (source == null) {
			throw new IllegalArgumentException("queueIncomingDataEvent: source is null!");
		}
		if (m_threads == null) {
			if (s_logger.isTraceDebugEnabled()) {
				int len = buffer.getMarkedBytesNumber();
				StringBuffer msg = new StringBuffer();
				msg.append('[');
				msg.append(len);
				msg.append("] bytes received from [");
				msg.append(source.toString());
				msg.append("]\n");

				boolean hide = SIPTransactionStack.instance().getConfiguration().hideAnything();
				if (hide) {
					msg.append("<raw packet is hidden>");
				}
				else {
					Debug.hexDump(buffer.getBytes(), 0, len, msg);
				}
				s_logger.traceDebug(msg.toString());
			}
			TransportCommLayerMgr.instance().onRead(buffer, source);
		}
		else {
			Event event = new IncomingDataEvent(buffer, source);
			queue(event);
		}
	}

	/**
	 * queues a new event when a connection gets closed
	 * 
	 * @param connection connection that was closed
	 */
	public void queueConnectionClosedEvent(SIPConnection connection) {
		if (m_threads == null) {
			// execute directly from the network thread
			TransportCommLayerMgr.instance().onConnectionClosed(connection);
		}
		else {
			Event event = new ConnectionClosedEvent(connection);
			queue(event);
		}
	}

	/**
	 * queues a new event when a new connection is accepted
	 * 
	 * @param listener listener that accepted the new connection
	 * @param connection new connection that is accepted
	 */
	public void queueConnectionAcceptedEvent(
		SIPListenningConnection listener,
		SIPConnection connection)
	{
		if (m_threads == null) {
			// execute directly from the network thread
			TransportCommLayerMgr.instance().onConnectionCreated(listener, connection);
		}
		else {
			Event event = new ConnectionAcceptedEvent(listener, connection);
			queue(event);
		}
	}
	
	/**
	 * queues a timer event.
	 * normally this is called from the timer thread when time elapsed.
	 * @param event the timer that has elapsed
	 */
	public void queueTimerEvent(TimerEvent event) {
		if (m_threads == null) {
			try {
				//Retrieve the call ID from the event
				String callID = event.getCallId();
				//Set the call ID on the current thread (for error debugging)
				ThreadLocalStorage.setCallID(callID);
				if (s_logger.isTraceDebugEnabled()) {
					s_logger.traceDebug(this, "queueTimerEvent", "storing the call ID on the current thread: " + callID);
				}
				
				// execute directly from the timer thread
				event.onExecute();
			} catch (Throwable t) {
				if (s_logger.isTraceFailureEnabled()) {
					s_logger.traceFailure(this, "queueTimerEvent", "exception occured while executing timer event " + t);
				}
			} finally {
				//Remove the call ID from the current thread
				ThreadLocalStorage.setCallID(null);
			}
		}
		else {
			queue(event);
		}
	}
	
	/**
	 * indicates whether the queue has reached its max capacity
	 * @return true if queue has reached its max capacity
	 */
	public boolean isOverLoaded() {
		return m_isOverLoaded; 
	}

	/** gets a numeric event type given the event instance */
	private static int eventType(Event event) {
		int eventType;
		final String className = event.getClass().getName();
		if (className.endsWith(".ConnectionAcceptedEvent")) {
			eventType = 1;
		}
		else if (className.endsWith(".ConnectionClosedEvent")) {
			eventType = 2;
		}
		else if (className.endsWith(".IncomingDataEvent")) {
			eventType = 3;
		}
		else if (className.endsWith(".IncomingMessageEvent")) {
			eventType = 4;
		}
		else if (className.endsWith(".ApiTimer")) {
			eventType = 5;
		}
		else if (className.endsWith(".CancelTimer")) {
			eventType = 6;
		}
		else if (className.endsWith("CleanupTimer")) {
			eventType = 7;
		}
		else if (className.endsWith("TimerA")) {
			eventType = 8;
		}
		else if (className.endsWith("TimerAPI")) {
			eventType = 9;
		}
		else if (className.endsWith("TimerB")) {
			eventType = 10;
		}
		else if (className.endsWith("TimerD")) {
			eventType = 11;
		}
		else if (className.endsWith("TimerE")) {
			eventType = 12;
		}
		else if (className.endsWith("TimerF")) {
			eventType = 13;
		}
		else if (className.endsWith("TimerG")) {
			eventType = 14;
		}
		else if (className.endsWith("TimerH")) {
			eventType = 15;
		}
		else if (className.endsWith("TimerJ")) {
			eventType = 16;
		}
		else if (className.endsWith("TimerK")) {
			eventType = 17;
		}
		else {
			eventType = 0;
		}
		return eventType;
	}
	
	/**
	 * reports statistics periodically
	 */
	private void report(Event event) {
		long now = System.currentTimeMillis();
		if (now - s_lastReport < s_reportInterval) {
			return;
		}
		// time for stat report
		s_lastReport = now;
		int peak = s_peak;
		s_peak = 0;

		StringBuffer report = new StringBuffer(1024);
		int nEvents = m_events.size();
		report.append("dispatch [").append(nEvents);
		report.append('/').append(peak);
		report.append('/').append(s_nHistory).append(']');

		for (int i = 0; i < N_EVENT_TYPES; i++) {
			report.append(' ');
			report.append(i).append('-').append(s_nQueuedDist[i]);
			report.append('/').append(s_nHistoryDist[i]);
		}
		System.out.println(report);
	}
}
