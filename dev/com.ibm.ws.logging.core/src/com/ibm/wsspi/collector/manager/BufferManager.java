/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.collector.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;


/**
 * Buffer manager is a wrapper around the actual buffer, it controls access to the buffer.
 * It also keeps track of information related to each handler like events consumed by the handler, next event for the
 * handler etc.
 */
public abstract class BufferManager {

    protected volatile static List<BufferManager> bufferManagerList= Collections.synchronizedList(new ArrayList<BufferManager>());
    protected Queue<Object> earlyMessageQueue;

    protected BufferManager() {
        synchronized(bufferManagerList) {
            bufferManagerList.add(this);
        }
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
