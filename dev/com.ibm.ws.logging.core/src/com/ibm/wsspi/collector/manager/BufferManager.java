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
package com.ibm.wsspi.collector.manager;

import com.ibm.ws.collector.manager.buffer.Buffer;

/**
 * Buffer manager is a wrapper around the actual buffer, it controls access to the buffer.
 * It also keeps track of information related to each handler like events consumed by the handler, next event for the
 * handler etc.
 */
public abstract class BufferManager {

    /* Reference to ring buffer implementation */
    protected final Buffer<Object> ringBuffer;

    protected BufferManager(int capacity) {
        ringBuffer = new Buffer<Object>(capacity);
    }

    /**
     * Method for adding an event to the buffer
     * Sources will use this method to add events to the buffer.
     * 
     * @param event Event that will be added to the buffer.
     */
    public abstract void add(Object event);

    /**
     * Method to retrieve the next event for a handler.
     * This is a blocking method, will block if no more events are available for the handler.
     * 
     * @param handlerId Handler Id.
     * @return Next event in the buffer for this handler.
     * @throws InterruptedException
     */
    public abstract Object getNextEvent(String handlerId) throws InterruptedException;

    /**
     * Method to retrieve the next n number of events for the handler.
     * This is a blocking method, will block if no more events are available for the handler.
     * <br>Note: If the number of events available from the handler is m and n > m,
     * only return m number of events.
     * 
     * @param handlerId Handler Id.
     * @param noOfEvents Number of events to retrieve.
     * @return Array containing the retrieved events.
     * @throws InterruptedException
     */
    public abstract Object[] getEvents(String handlerId, int noOfEvents) throws InterruptedException;
}
