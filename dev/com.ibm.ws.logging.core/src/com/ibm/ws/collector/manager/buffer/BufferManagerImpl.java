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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

public class BufferManagerImpl extends BufferManager {

	private static final TraceComponent tc = Tr.register(BufferManagerImpl.class);	
    private Buffer<Object> ringBuffer;
	private static final ReentrantReadWriteLock RERWLOCK = new ReentrantReadWriteLock(true);
	private Set<SynchronousHandler> synchronizedHandlerSet = new HashSet<SynchronousHandler>();
	private int capacity;

	private final String sourceId;
	/* Map to keep track of the next event for a handler */
	private final ConcurrentHashMap<String, HandlerStats> handlerEventMap = new ConcurrentHashMap<String, HandlerStats>();

	public BufferManagerImpl(int capacity, String sourceId) {
		super();
		ringBuffer=null;
		this.sourceId = sourceId;
		this.capacity = capacity;
	}
	
	@Override
	@FFDCIgnore({ NullPointerException.class })
	public void add(Object event) {
		if (event == null)
			throw new NullPointerException();

		RERWLOCK.readLock().lock();
		try {
			
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
				for (SynchronousHandler synchronizedHandler : synchronizedHandlerSet) {
					synchronizedHandler.synchronousWrite(event);
				}
				
			}
			try {
				if(ringBuffer !=  null){
					ringBuffer.add(event);
				}
			}catch(NullPointerException e){
			}
			
			try {
				if(earlyMessageQueue!=null) {
					earlyMessageQueue.add(event);
				}
			}catch(NullPointerException e){
			}
			
		} finally {
				RERWLOCK.readLock().unlock();
		}
		
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.debug(tc, "Adding event to buffer " + event);
		}
		
	}
	
	@Override
	public void removeEMQ() {
		System.out.println("Removing EMQ: " + this.sourceId);
		earlyMessageQueue=null;
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
		RERWLOCK.writeLock().lock();
		try {
			//If it is first async handler subscribed, then create the main buffer
			if(ringBuffer == null) {
				ringBuffer = new Buffer<Object>(capacity);
			}
			/*
			 * Every new Asynchronous handler starts off with all events from EMQ.
			 * So we create a copy of current EMQ and sends those messages to RingBuffer
			 */
			if(earlyMessageQueue != null && !earlyMessageQueue.isEmpty()) {
				System.out.println("Sending Early Messages to New Asynchronous Handler: " + handlerId);
				Object holder[] = new Object[EARLY_MESSAGE_QUEUE_SIZE];
				Object[] messagesList = earlyMessageQueue.toArray(holder);
				for(Object message: messagesList) {
					ringBuffer.add(message);
				}
			}
			handlerEventMap.putIfAbsent(handlerId, new HandlerStats(handlerId, sourceId));
		}finally {
			RERWLOCK.writeLock().unlock();
		}
	}

	public void addSyncHandler(SynchronousHandler syncHandler) {
		/*
		 * There can be many Reader locks, but only one writer lock. This
		 * ReaderWriter lock is needed to avoid CMException when the add()
		 * method is forwarding log events to synchronized handlers and an
		 * addSyncHandler or removeSyncHandler is called
		 */
		RERWLOCK.writeLock().lock();
		try {
			//Send messages from EMQ to synchronous handler when it subscribes to receive messages
			if(earlyMessageQueue != null && !earlyMessageQueue.isEmpty() && !synchronizedHandlerSet.contains(syncHandler)) {
				System.out.println("Sending Early Messages to New Synchronized Handler: " + syncHandler.getHandlerName());
				Object holder[] = new Object[EARLY_MESSAGE_QUEUE_SIZE];
				Object[] messagesList = earlyMessageQueue.toArray(holder);
				for(Object message: messagesList) {
					syncHandler.synchronousWrite(message);
				}
			}
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
		RERWLOCK.writeLock().lock();
		try {
			handlerEventMap.remove(handlerId);
			if(handlerEventMap.isEmpty()) {
				ringBuffer=null;
			}
		} finally {
			RERWLOCK.writeLock().unlock();
		}
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
