/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.buffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

public class BufferManagerImpl extends BufferManager {

    /* Package name in trace from BufferManagerImpl is changed in order to reduce the trace volume when traceSpecification is set to "com.ibm.ws.*" */
    private static final TraceComponent tc = Tr.register("x.com.ibm.ws.collector.manager.buffer.BufferManagerImpl",BufferManagerImpl.class,(String)null);	
    private Buffer<Object> ringBuffer;
    private Set<SynchronousHandler> synchronousHandlerSet = new HashSet<SynchronousHandler>();

	private final int capacity;

	private final String sourceId;
	/* Map to keep track of the next event for a handler */
	private final ConcurrentHashMap<String, HandlerStats> handlerEventMap = new ConcurrentHashMap<String, HandlerStats>();

	protected Queue<Object> earlyMessageQueue;

	private static final int EARLY_MESSAGE_QUEUE_SIZE = 400;
	
	public BufferManagerImpl(int capacity, String sourceId) {
		this(capacity,sourceId, true);
	}

	public BufferManagerImpl(int capacity, String sourceId, boolean isSoftRefEMQ) {
		super();

		BufferManagerEMQHelper.addBufferManagerList(this);
		ringBuffer = null;
		this.sourceId = sourceId;
		this.capacity = capacity;
		if (!BufferManagerEMQHelper.getEMQRemovedFlag()) {
			if (isSoftRefEMQ){
				earlyMessageQueue = new SimpleRotatingSoftQueue<Object>(new Object[EARLY_MESSAGE_QUEUE_SIZE]);
			} else {
				earlyMessageQueue = new SimpleRotatingQueue<Object>(new Object[EARLY_MESSAGE_QUEUE_SIZE]);
			}
			// Check again just in case
			if (BufferManagerEMQHelper.getEMQRemovedFlag()) {
				removeEMQ();
			}
		}
	}
	
	@Override
	public void add(Object event) {
		if (event == null)
			throw new NullPointerException();

		if (earlyMessageQueue != null) { // startup time
			/*
			 * earlyMessageQueue was first checked to be not null but this "gap"
			 * here may have allowed the earlyMessageQueue to be set to null
			 * from removeEMQ()
			 */
			Set<SynchronousHandler> synchronousHandlerSetSnapShot;

			synchronized (this) {
				// Must check again - could have been removed
				if (earlyMessageQueue != null) {
					earlyMessageQueue.add(event);
				}

				// If async handler added before this synchronized block, need
				// to add to ring buffer
				if (ringBuffer != null) {
					ringBuffer.add(event);
				}
				synchronousHandlerSetSnapShot = synchronousHandlerSet;
			}

			/*
			 * Cannot put in synchronize block due to a deadlock, but under the
			 * assumption it "could have been" placed in the synchronized block,
			 * we would have sent to this snapshot of synchronous handlers
			 */
			for (SynchronousHandler synchronousHandler : synchronousHandlerSetSnapShot) {
				synchronousHandler.synchronousWrite(event);
			}
		} else { // after startup
			/*
			 * Get the latest up to date synchronousHandlerSet until we start
			 * looping
			 */
			for (SynchronousHandler synchronousHandler : synchronousHandlerSet) {
				synchronousHandler.synchronousWrite(event);
			}

			if (ringBuffer != null) {
				addEventToRingBuffer(event);
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Adding event to buffer " + event);
		}
	}

	/**
	 * Method to add events to the ringBufferthat and ignores a possible NPE
	 * with ringBuffer which is due to the removeHandler method call from this
	 * same class
	 * 
	 * We do not wish synchronize the add for every ringBuffer due to
	 * performance impacts
	 * 
	 * @param event
	 *            event to add to the buffer
	 */
	@FFDCIgnore(NullPointerException.class)
	private void addEventToRingBuffer(Object event) {
		// Check again to see if the ringBuffer is null
		if (ringBuffer != null) {
			try {
				ringBuffer.add(event);
			} catch (NullPointerException npe) {
				// Nothing to do! Perhaps a Trace?
			}
		}
	}

	@Override
	public Object getNextEvent(String handlerId) throws InterruptedException {
		HandlerStats handlerStats = null;
		if (handlerId != null) {
			handlerStats = handlerEventMap.get(handlerId);
		}
		if (handlerStats == null)
			throw new IllegalArgumentException("Handler not registered with buffer manager : " + handlerId);
		// Get the next sequence number for this handler
		long seqNum = handlerStats.getNextSeqNum();
		Event<Object> event = ringBuffer.get(seqNum);
		handlerStats.traceEventLoss(event.getSeqNum());
		// Calculate the next sequence number based on the sequence number
		// of the retrieved event.
		long nextSeqNum = event.getSeqNum() + 1;
		handlerStats.setNextSeqNum(nextSeqNum);
		return event.getEvent();
	}

	@Override
	public Object[] getEvents(String handlerId, int noOfEvents) throws InterruptedException {
		HandlerStats handlerStats = null;
		if (handlerId != null) {
			handlerStats = handlerEventMap.get(handlerId);
		}
		if (handlerStats == null)
			throw new IllegalArgumentException("Handler not registered with buffer manager : " + handlerId);
		// Get the next sequence number for this handler
		long seqNum = handlerStats.getNextSeqNum();
		ArrayList<Event<Object>> events = ringBuffer.get(seqNum, noOfEvents);
		handlerStats.traceEventLoss(events.get(0).getSeqNum());
		// Calculate the next sequence number based on the sequence numbers
		// of the retrieved events.
		long nextSeqNum = events.get(0).getSeqNum() + events.size();
		handlerStats.setNextSeqNum(nextSeqNum);
		Object[] e = new Object[events.size()];
		for (int i = 0; i < events.size(); i++) {
			e[i] = events.get(i).getEvent();
		}
		return e;
	}

	/**
	 * Add an asynchronous handler that will consume events from this 
	 * BufferManagerImpl/Conduit's buffer
	 * 
	 * @param handlerId handlerID to add to this BufferManager
	 */
	public synchronized void addHandler(String handlerId) {
		// If it is first async handler subscribed, then create the main buffer
		if (ringBuffer == null) {
			ringBuffer = new Buffer<Object>(capacity);
		}
		/*
		 * Every new Asynchronous handler starts off with all events from EMQ.
		 * So we write all EMQ messages directly to RingBuffer
		 */

		if (earlyMessageQueue != null && earlyMessageQueue.size() != 0) {
			for (Object message : earlyMessageQueue.toArray()) {
				if (message != null) {
					ringBuffer.add(message);
				}
			}
		}

		handlerEventMap.putIfAbsent(handlerId, new HandlerStats(handlerId, sourceId));
		Tr.event(tc, "Added Asynchronous Handler: " + handlerId);
	}

	/**
	 * Add a synchronousHandler that will receive log events directly
	 * 
	 * @param syncHandler synchronousHandler that will receive log events directly
	 */
	public synchronized void addSyncHandler(SynchronousHandler syncHandler) {
		// Send messages from EMQ to synchronous handler when it subscribes to
		// receive messages
		if (earlyMessageQueue != null && earlyMessageQueue.size() != 0
				&& !synchronousHandlerSet.contains(syncHandler)) {
			for (Object message : earlyMessageQueue.toArray()) {
				if (message != null){
					syncHandler.synchronousWrite(message);
				}
			}
		}

		Set<SynchronousHandler> synchronousHandlerSetCopy = new HashSet<SynchronousHandler>(synchronousHandlerSet);
		synchronousHandlerSetCopy.add(syncHandler);
		Tr.event(tc, "Added Synchronous Handler: " + syncHandler.getHandlerName());
		synchronousHandlerSet = synchronousHandlerSetCopy;
	}

	/**
	 * Remove a synchronousHandler from receiving log events directly
	 * 
	 * @param syncHandler syncHandler to remove
	 */
	public synchronized void removeSyncHandler(SynchronousHandler syncHandler) {
		Set<SynchronousHandler> synchronousHandlerSetCopy = new HashSet<SynchronousHandler>(synchronousHandlerSet);
		synchronousHandlerSetCopy.remove(syncHandler);
		Tr.event(tc, "Removed Synchronous Handler: " + syncHandler.getHandlerName());
		synchronousHandlerSet = synchronousHandlerSetCopy;
	}

	/**
	 * Remove the given handlerId from this BufferManager
	 * 
	 * @param handlerId handlerId to remove
	 */
	public synchronized void removeHandler(String handlerId) {
		handlerEventMap.remove(handlerId);
		Tr.event(tc, "Removed Asynchronous Handler: " + handlerId);
		if (handlerEventMap.isEmpty()) {
			ringBuffer = null;
			Tr.event(tc, "ringBuffer for this BufferManagerImpl has now been set to null");
		}
	}

	public synchronized void removeEMQ() {
		earlyMessageQueue = null;
	}

	public static class HandlerStats {

		private final String handlerId;
		private final String sourceId;
		private long seqNum;

		// Variables to keep track of lost events
		private long lostEventsForTrace;
		private long lostEventsForWarning;
		private long totalLostEvents;

		private long lastReportTimeForTrace;
		private long lastReportTimeForWarning;
		private final long intervalForTrace = 60 * 1000;
		private final long intervalForWarning = 60 * 5 * 1000;

		public HandlerStats(String handlerId, String sourceId) {
			this.handlerId = handlerId;
			this.sourceId = sourceId;
			seqNum = 1;
			lostEventsForWarning = lostEventsForTrace = totalLostEvents = 0;
			lastReportTimeForWarning = 0;
			lastReportTimeForTrace = System.currentTimeMillis();
		}

		public long getNextSeqNum() {
			return seqNum;
		}

		public void setNextSeqNum(long nextSeqNum) {
			this.seqNum = nextSeqNum;
		}

		public void traceEventLoss(long retSeqNum) {
			if (retSeqNum > seqNum) {
				long eventsLost = retSeqNum - seqNum;
				lostEventsForWarning += eventsLost;
				lostEventsForTrace += eventsLost;
				totalLostEvents += eventsLost;
			}
			long currentTime = System.currentTimeMillis();
			long timeElapsed = currentTime - lastReportTimeForWarning;
			if (lostEventsForWarning > 0 && timeElapsed >= intervalForWarning) {
				if (lastReportTimeForWarning == 0) {
					// This was the first instance where we started to see loss
					// of events.
					Tr.warning(tc, "HANDLER_STARTED_TO_LOSE_EVENTS_WARNING", handlerId, lostEventsForWarning, sourceId);
				} else {
					long timeElapsedInMins = TimeUnit.MILLISECONDS.toMinutes(timeElapsed);
					Tr.warning(tc, "HANDLER_LOST_EVENTS_WARNING", handlerId, lostEventsForWarning, sourceId,
							timeElapsedInMins, totalLostEvents);
				}
				lastReportTimeForWarning = currentTime;
				lostEventsForWarning = 0;
			}
			timeElapsed = currentTime - lastReportTimeForTrace;
			if (timeElapsed >= intervalForTrace) {
				if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
					long timeElapsedInSecs = TimeUnit.MILLISECONDS.toSeconds(timeElapsed);
					Tr.event(tc,
							"Handler [{0}] has lost {1} events from source [{2}] in the last {3} second(s), and has lost {4} events from the source since the handler started.",
							handlerId, lostEventsForTrace, sourceId, timeElapsedInSecs, totalLostEvents);
				}
				lastReportTimeForTrace = currentTime;
				lostEventsForTrace = 0;
			}
		}

	}
}
