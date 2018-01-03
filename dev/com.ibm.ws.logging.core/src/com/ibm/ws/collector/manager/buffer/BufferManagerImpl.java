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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

public class BufferManagerImpl extends BufferManager {

	private static final TraceComponent tc = Tr.register(BufferManagerImpl.class);
	private static final ReentrantReadWriteLock RERWLOCK = new ReentrantReadWriteLock(true);
	private Set<SynchronousHandler> synchronizedHandlerSet = new HashSet<SynchronousHandler>();

	private final String sourceId;
	/* Map to keep track of the next event for a handler */
	private final ConcurrentHashMap<String, HandlerStats> handlerEventMap = new ConcurrentHashMap<String, HandlerStats>();

	public BufferManagerImpl(int capacity, String sourceId) {
		super(capacity);
		this.sourceId = sourceId;
	}

	@Override
	public void add(Object event) {
		if (event == null)
			throw new NullPointerException();

		/*
		 * Check if we have any synchronized handlers, and write directly to
		 * them
		 */
		if (!synchronizedHandlerSet.isEmpty()) {
			/*
			 * There can be many Reader locks, but only one writer lock. This
			 * ReaderWriter lock is needed to avoid CMException when the add()
			 * method is forwarding log events to synchronized handlers and an
			 * addSyncHandler or removeSyncHandler is called
			 */
			RERWLOCK.readLock().lock();
			try {
				for (SynchronousHandler synchronizedHandler : synchronizedHandlerSet) {
					synchronizedHandler.synchronousWrite(event);
				}
			} finally {
				RERWLOCK.readLock().unlock();
			}
		}

		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Adding event to buffer " + event);
		}
		ringBuffer.add(event);
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

	public void addHandler(String handlerId) {
		handlerEventMap.putIfAbsent(handlerId, new HandlerStats(handlerId, sourceId));
	}

	public void addSyncHandler(SynchronousHandler syncHandler) {
        /* There can be many Reader locks, but only one writer lock.
        *  This ReaderWriter lock is needed to avoid CMException when the add() method is forwarding log events
        *  to synchronized handlers and an addSyncHandler or removeSyncHandler is called
        */
		RERWLOCK.writeLock().lock();
		try{
			synchronizedHandlerSet.add(syncHandler);
		} finally {
        RERWLOCK.writeLock().unlock();
    	}
	}
	
	public void removeSyncHandler(SynchronousHandler syncHandler) {
        /*
		 * There can be many Reader locks, but only one writer lock. This
		 * ReaderWriter lock is needed to avoid CMException when the add()
		 * method is forwarding log events to synchronized handlers and an
		 * addSyncHandler or removeSyncHandler is called
		 */
		RERWLOCK.writeLock().lock();
		try {
			synchronizedHandlerSet.remove(syncHandler);
		} finally {
			RERWLOCK.writeLock().unlock();
		}
	}

	public void removeHandler(String handlerId) {
		handlerEventMap.remove(handlerId);
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
