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
