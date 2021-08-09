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
package com.ibm.ws.collector.manager.buffer;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Class for holding events, ring buffer will be initialized with objects of Event class on creation.
 * Marked as trivial since otherwise will cause a infinite loop with logging f/w.
 */
@Trivial
public class Event<T> {

    //Sequence number of this event
    private long seqNum;

    //Reference to the actual event
    private T event;

    public Event(long seqNum, T event) {
        this.seqNum = seqNum;
        this.event = event;
    }

    /**
     * Copy constructor
     */
    public Event(Event<T> e) {
        this.seqNum = e.seqNum;
        this.event = e.event;
    }

    public long getSeqNum() {
        return seqNum;
    }

    public void setSeqNum(long seqNum) {
        this.seqNum = seqNum;
    }

    public T getEvent() {
        return event;
    }

    public void setEvent(T event) {
        this.event = event;
    }

//    @Override
//    public Object clone() {
//        try {
//            return super.clone();
//        } catch (CloneNotSupportedException e) {
//            return null;
//        }
//    }

    @Override
    public String toString() {
        return "Event [seqNum=" + seqNum + ", event=" + event + "]";
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((event == null) ? 0 : event.hashCode());
        result = prime * result + (int) (seqNum ^ (seqNum >>> 32));
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event<?> other = (Event<?>) obj;
        if (event == null) {
            if (other.event != null)
                return false;
        } else if (!event.equals(other.event))
            return false;
        if (seqNum != other.seqNum)
            return false;
        return true;
    }

}
