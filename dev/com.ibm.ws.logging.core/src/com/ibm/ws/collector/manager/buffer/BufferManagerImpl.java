/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.collector.manager.buffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.logging.synch.ThreadLocalHandler;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.SyncrhonousHandler;

public class BufferManagerImpl extends BufferManager {

	private static final TraceComponent tc = Tr.register(BufferManagerImpl.class);

	// DYKC
	private Set<SyncrhonousHandler> synchronizedHandlerSet = new HashSet<SyncrhonousHandler>();

	private final String sourceId;
	/* Map to keep track of the next event for a handler */
	private final ConcurrentHashMap<String, HandlerStats> handlerEventMap = new ConcurrentHashMap<String, HandlerStats>();

	public BufferManagerImpl(int capacity, String sourceId) {
		super(capacity);
		this.sourceId = sourceId;
	}

	@Override
	public void add(Object event) {
		// DYKC
		// Check if we have any synchronized handlers, and write directly to
		// them.
		if (!synchronizedHandlerSet.isEmpty()) {
			for (SyncrhonousHandler synchronizedHandler : synchronizedHandlerSet) {
				synchronizedHandler.synchronousWrite(event);
			}
		}
		if (event == null)
			throw new NullPointerException();
		//DYCK- effectively reoves it from JSON output, but is still there for normal trace.log
		ThreadLocalHandler.set(Boolean.TRUE);
		try {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
				Tr.debug(tc, "Adding event to buffer " + event);
			}
			ringBuffer.add(event);
		} finally {
			ThreadLocalHandler.remove();
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

	public void addHandler(String handlerId) {
		handlerEventMap.putIfAbsent(handlerId, new HandlerStats(handlerId, sourceId));
	}

	public void addSyncHandler(SyncrhonousHandler syncHandler) {
		// DYKC
		System.out.println("Adding a syncrhnous handler " + syncHandler.getHandlerName());
		synchronizedHandlerSet.add(syncHandler);
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
