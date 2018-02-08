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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
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
    private Set<SynchronousHandler> synchronousHandlerSet = new HashSet<SynchronousHandler>();
    private final int capacity;

    private final String sourceId;
    /* Map to keep track of the next event for a handler */
    private final ConcurrentHashMap<String, HandlerStats> handlerEventMap = new ConcurrentHashMap<String, HandlerStats>();
    private static List<BufferManager> bufferManagerList= new ArrayList<BufferManager>();
    
    private Queue<Object> earlyMessageQueue;
    private volatile static boolean EMQRemovedFlag = false;
    private static final int EARLY_MESSAGE_QUEUE_SIZE=400;
    private static final int EMQ_TIMER = 60 * 5 * 1000; //5 Minute timer

    public BufferManagerImpl(int capacity, String sourceId) {
        super();
        RERWLOCK.writeLock().lock();
        try {
            bufferManagerList.add(this);
            ringBuffer=null;
            this.sourceId = sourceId;
            this.capacity = capacity;
            if(!BufferManagerImpl.EMQRemovedFlag)
                earlyMessageQueue = new SimpleRotatingSoftQueue<Object>(new Object[EARLY_MESSAGE_QUEUE_SIZE]);
        }finally {
            RERWLOCK.writeLock().unlock();
        }
    }

    @Override
    public void add(Object event) {
        if (event == null)
            throw new NullPointerException();

        SynchronousHandler[] arrayCopy = null;
        RERWLOCK.readLock().lock();
        try {

            /*
             * Check if we have any synchronous handlers, and write directly to
             * them
             */
            if (!synchronousHandlerSet.isEmpty()) {
                /*
                 * There can be many Reader locks, but only one writer lock. This
                 * ReaderWriter lock is needed to avoid CMException when the add()
                 * method is forwarding log events to synchronous handlers and an
                 * addSyncHandler or removeSyncHandler is called
                 */
            	arrayCopy = synchronousHandlerSet.toArray(new SynchronousHandler[0]);
            }
            
            if(ringBuffer !=  null){
                ringBuffer.add(event);
            }
            
            if(earlyMessageQueue!=null) {
                synchronized(earlyMessageQueue) {
                    earlyMessageQueue.add(event);
                }
            }

        } finally {
            RERWLOCK.readLock().unlock();
            if (arrayCopy != null){
	                for (SynchronousHandler synchronousHandler : arrayCopy) {
	                	synchronousHandler.synchronousWrite(event);
	                }
	            }
	            arrayCopy = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Adding event to buffer " + event);
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
        RERWLOCK.writeLock().lock();
        try {
            //If it is first async handler subscribed, then create the main buffer
            if(ringBuffer == null) {
                ringBuffer = new Buffer<Object>(capacity);
            }
            /*
             * Every new Asynchronous handler starts off with all events from EMQ.
             * So we write all EMQ messages directly to RingBuffer
             */
            if(earlyMessageQueue != null && earlyMessageQueue.size() != 0) {
                for(Object message: earlyMessageQueue.toArray()) {
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
         * method is forwarding log events to synchronous handlers and an
         * addSyncHandler or removeSyncHandler is called
         */
        RERWLOCK.writeLock().lock();
        try {
            //Send messages from EMQ to synchronous handler when it subscribes to receive messages
            if(earlyMessageQueue != null && earlyMessageQueue.size() != 0 && !synchronousHandlerSet.contains(syncHandler)) {
                for(Object message: earlyMessageQueue.toArray()) {
                    syncHandler.synchronousWrite(message);
                }
            }
            synchronousHandlerSet.add(syncHandler);
        } finally {
            RERWLOCK.writeLock().unlock();
        }
    }

    public void removeSyncHandler(SynchronousHandler syncHandler) {
        /*
         * There can be many Reader locks, but only one writer lock. This
         * ReaderWriter lock is needed to avoid CMException when the add()
         * method is forwarding log events to synchronous handlers and an
         * addSyncHandler or removeSyncHandler is called
         */
        RERWLOCK.writeLock().lock();
        try {
            synchronousHandlerSet.remove(syncHandler);
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
    
    public void removeEMQ() {
        RERWLOCK.writeLock().lock();
        try {
            earlyMessageQueue=null;
        }finally {
            RERWLOCK.writeLock().unlock();
        }
    }

    public static void removeEMQTrigger(){
        RERWLOCK.writeLock().lock();
        try {
            EMQRemovedFlag=true;
            for(BufferManager i: bufferManagerList) {
                ((BufferManagerImpl) i).removeEMQ();
            }
        }finally {
            RERWLOCK.writeLock().unlock();
        }
    }

    public static void removeEMQByTimer(){
        new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        BufferManagerImpl.removeEMQTrigger();
                    }
                },
                BufferManagerImpl.EMQ_TIMER);
    }
    
    public static boolean getEMQRemovedFlag() {
        return EMQRemovedFlag;
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
