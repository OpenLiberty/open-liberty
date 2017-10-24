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
package com.ibm.ws.collector.manager.internal.buffer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Ring buffer implementation
 * Marked as trivial since otherwise will cause a infinite loop with logging f/w.
 */
@Trivial
public class Buffer<T> {

    //No of events the buffer can store.
    private final int capacity;

    //Array used for storing events.
    private final ArrayList<Event<T>> ringBuffer;

    /*
     * Current sequence number is used to keep track of next slot in the buffer that will be
     * used for storing a new event.
     *
     * Earliest sequence number is used to keep track of oldest event available in the buffer
     */
    private long currentSeqNum;

    private long earliestSeqNum;

    /*
     * Locks used for controlling access to the buffer
     * 1) Read lock is used to get events from the buffer, using read lock
     * allows multiple readers to access the buffer simultaneously.
     * 2) Write lock is used to add events to the buffer.
     */

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    private final Lock w = rwl.writeLock();

    private final Lock r = rwl.readLock();

    private final Condition condition = w.newCondition();

    public Buffer(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Buffer capacity has to be >= 1");
        }
        //Initialize the array with required capacity
        ringBuffer = new ArrayList<Event<T>>(Collections.nCopies(capacity, new Event<T>(-1, null)));
        this.capacity = capacity;
        //Set current and earliest sequence number to 1
        currentSeqNum = earliestSeqNum = 1;
    }

    /**
     * @param event
     */
    public void add(T event) {
        w.lock();
        try {
            //Check if we are over-writing an earlier event, if so increment the
            //earliest sequence number.
            if (currentSeqNum > capacity) {
                earliestSeqNum++;
            }
            //Get the index of the slot corresponding to current sequence number
            int index = (int) ((currentSeqNum - 1) % capacity);
            //Assign the sequence number to the event object and add the event
            if (ringBuffer.get(index).getEvent() == null) {
                ringBuffer.set(index, new Event<T>(currentSeqNum, event));
            } else {
                ringBuffer.get(index).setSeqNum(currentSeqNum);
                ringBuffer.get(index).setEvent(event);
            }

            //Increment the current sequence number
            currentSeqNum++;

            //Notify all threads waiting for new events.
            condition.signalAll();
        } finally {
            w.unlock();
        }
    }

    /**
     * @param seqNum
     * @return
     * @throws InterruptedException
     */
    public Event<T> get(long seqNum) throws InterruptedException {
        r.lock();
        //Trying to read a sequence number that is yet to be associated
        //with an event or if the buffer is empty
        while (seqNum >= currentSeqNum || currentSeqNum == earliestSeqNum) {
            //Release the read lock and acquire the write lock
            r.unlock();
            w.lock();
            try {
                //Re-check the state of the buffer because another thread might have
                //acquired the write lock and changed the state.
                if (seqNum >= currentSeqNum || currentSeqNum == earliestSeqNum) {
                    //Block this thread till an event is available
                    condition.await();
                }
                //Downgrade by acquiring a read lock before releasing the write lock
                r.lock();
            } finally {
                w.unlock(); //Unlock write, still hold read
            }
        }

        try {
            //If the event associated with the sequence number has been over-written
            //Move the sequence number to that of the earliest event in the buffer.
            if (seqNum < earliestSeqNum) {
                seqNum = earliestSeqNum;
            }
            //Get the index of the slot corresponding to the sequence number
            int index = (int) ((seqNum - 1) % capacity);
            //return (Event<T>) ringBuffer.get(index).clone();
            return new Event<T>(ringBuffer.get(index));
        } finally {
            r.unlock();
        }
    }

    /**
     * @param seqNum
     * @param noOfEvents
     * @return
     * @throws InterruptedException
     */
    public ArrayList<Event<T>> get(long seqNum, int noOfEvents) throws InterruptedException {
        r.lock();
        //Trying to read a sequence number that is yet to be associated
        //with an event or if the buffer is empty
        while (seqNum >= currentSeqNum || currentSeqNum == earliestSeqNum) {
            //Release the read lock and acquire the write lock
            r.unlock();
            w.lock();
            try {
                //Re-check the state of the buffer because another thread might have
                //acquired the write lock and changed the state.
                if (seqNum >= currentSeqNum || currentSeqNum == earliestSeqNum) {
                    //Block this thread till an event is available
                    condition.await();
                }
                //Downgrade by acquiring a read lock before releasing the write lock
                r.lock();
            } finally {
                w.unlock(); //Unlock write, still hold read
            }
        }

        try {
            //If the event associated with the sequence number has been over-written
            //Move the sequence number to that of the earliest event in the buffer.
            if (seqNum < earliestSeqNum) {
                seqNum = earliestSeqNum;
            }
            //Calculate the number of available events
            int noOfAvailableEvents = (int) (currentSeqNum - seqNum);

            if (noOfAvailableEvents < noOfEvents) {
                noOfEvents = noOfAvailableEvents;
            }
            return copyEvents(seqNum, noOfEvents);
        } finally {
            r.unlock();
        }
    }

    private ArrayList<Event<T>> copyEvents(long seqNum, int noOfEvents) {
        ArrayList<Event<T>> events = new ArrayList<Event<T>>(noOfEvents);
        //Calculate the index of the interested event
        int index = (int) ((seqNum - 1) % capacity);
        for (int i = index, j = 0; j < noOfEvents; i = (i + 1) % capacity, j++) {
            //events.add((Event<T>) ringBuffer.get(i).clone());
            events.add(new Event<T>(ringBuffer.get(i)));
        }
        return events;
    }

    /**
     * Should be used only for testing. This method is not thread safe.
     */
//    public void dumpBuffer() {
//        System.out.println("------------------- Dumping Buffer ----------------------");
//        System.out.println("Sequence Numbers [currentSeqNum=" + currentSeqNum + ", earliestSeqNum=" + earliestSeqNum + "]");
//        for (Event<T> event : ringBuffer) {
//            System.out.print(event);
//            System.out.print(" || ");
//        }
//        System.out.println();
//        System.out.println("---------------------------------------------------------");
//    }
}
